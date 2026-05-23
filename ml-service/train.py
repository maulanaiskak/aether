"""
Train a LightGBM model on PM2.5 historical data.
Generates model.pkl and metrics.json at build time.

NOTE: Training data is sourced from Open-Meteo CAMS modelled forecasts,
not independent observations. The model acts as a surrogate/nowcast layer,
not an attempt to beat the CAMS source. See /metrics for MAE vs naive baseline.
"""
import json
import os
import numpy as np
import pandas as pd
import joblib
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error

try:
    import lightgbm as lgb
    USE_LGB = True
except ImportError:
    USE_LGB = False

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
MODEL_PATH = os.path.join(os.path.dirname(__file__), "model.pkl")
METRICS_PATH = os.path.join(os.path.dirname(__file__), "metrics.json")
HORIZON = 24


def load_or_generate_data():
    fixture = os.path.join(DATA_DIR, "training_fixture.csv")
    if os.path.exists(fixture):
        return pd.read_csv(fixture, index_col=0, parse_dates=True)

    # Generate synthetic data
    np.random.seed(42)
    periods = 24 * 365
    idx = pd.date_range("2024-01-01", periods=periods, freq="h")
    pm25 = (20 + 10 * np.sin(np.arange(periods) * 2 * np.pi / 24)
            + 5 * np.random.randn(periods)).clip(0)
    df = pd.DataFrame({
        "pm2_5": pm25,
        "temperature": 28 + 5 * np.sin(np.arange(periods) * 2 * np.pi / 24) + np.random.randn(periods),
        "humidity": 70 + 10 * np.cos(np.arange(periods) * 2 * np.pi / 24) + np.random.randn(periods),
        "wind_speed": (5 + 3 * np.random.randn(periods)).clip(0),
    }, index=idx)

    os.makedirs(DATA_DIR, exist_ok=True)
    df.to_csv(fixture)
    return df


def engineer_features(df):
    for lag in range(1, 25):
        df[f"pm2_5_lag{lag}"] = df["pm2_5"].shift(lag)
    df["hour"] = df.index.hour
    df["dow"] = df.index.dayofweek
    return df.dropna()


def train():
    df = load_or_generate_data()
    df = engineer_features(df)

    feature_cols = [c for c in df.columns if c != "pm2_5"]
    X, y = df[feature_cols], df["pm2_5"]

    split = int(len(df) * 0.8)
    X_train, X_test = X.iloc[:split], X.iloc[split:]
    y_train, y_test = y.iloc[:split], y.iloc[split:]

    if USE_LGB:
        model = lgb.LGBMRegressor(n_estimators=200, learning_rate=0.05, num_leaves=31)
        model_name = "lgbm"
    else:
        model = GradientBoostingRegressor(n_estimators=200, learning_rate=0.05, max_depth=4)
        model_name = "sklearn_gb"

    model.fit(X_train, y_train)

    preds = model.predict(X_test)
    residuals = y_test.values - preds
    residual_std = float(np.std(residuals))

    # Naive persistence baseline
    naive_preds = y_test.shift(1).fillna(method="ffill").values
    naive_mae = float(mean_absolute_error(y_test.values[1:], naive_preds[1:]))
    naive_rmse = float(np.sqrt(mean_squared_error(y_test.values[1:], naive_preds[1:])))

    metrics = {
        "model": model_name,
        "mae": float(mean_absolute_error(y_test, preds)),
        "rmse": float(np.sqrt(mean_squared_error(y_test, preds))),
        "residual_std": residual_std,
        "baseline_mae": naive_mae,
        "baseline_rmse": naive_rmse,
    }

    joblib.dump({"model": model, "feature_cols": feature_cols,
                 "residual_std": residual_std, "model_name": model_name}, MODEL_PATH)
    with open(METRICS_PATH, "w") as f:
        json.dump(metrics, f, indent=2)

    print(f"Trained {model_name}: MAE={metrics['mae']:.2f}, RMSE={metrics['rmse']:.2f}")
    print(f"Baseline: MAE={naive_mae:.2f}, RMSE={naive_rmse:.2f}")


if __name__ == "__main__":
    train()
