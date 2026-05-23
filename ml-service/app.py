"""
FastAPI ML sidecar: PM2.5 short-horizon forecast.
"""
import json
import os
from datetime import datetime, timezone, timedelta
from typing import Any

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

MODEL_PATH = os.path.join(os.path.dirname(__file__), "model.pkl")
METRICS_PATH = os.path.join(os.path.dirname(__file__), "metrics.json")
HORIZON = 24

app = FastAPI(title="Aether ML Service")

_artifact: dict[str, Any] | None = None


def load_model():
    global _artifact
    if _artifact is None and os.path.exists(MODEL_PATH):
        _artifact = joblib.load(MODEL_PATH)
    return _artifact


class DataPoint(BaseModel):
    observedAt: str
    value: float | None = None


class ForecastRequest(BaseModel):
    location: str
    history: list[DataPoint]


class Prediction(BaseModel):
    horizonAt: str
    predicted: float
    lowerBound: float
    upperBound: float


class ForecastResponse(BaseModel):
    location: str
    model: str
    predictions: list[Prediction]


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/metrics")
def metrics():
    if not os.path.exists(METRICS_PATH):
        raise HTTPException(status_code=503, detail="Metrics not available — model not trained yet")
    with open(METRICS_PATH) as f:
        return json.load(f)


@app.post("/forecast", response_model=ForecastResponse)
def forecast(request: ForecastRequest):
    artifact = load_model()
    if artifact is None:
        raise HTTPException(status_code=503, detail="Model not ready")

    model = artifact["model"]
    feature_cols = artifact["feature_cols"]
    residual_std = artifact.get("residual_std", 5.0)
    model_name = artifact.get("model_name", "lgbm")

    # Build feature vector from last 24 history points
    values = [p.value or 0.0 for p in request.history[-24:]]
    # Pad if insufficient history
    while len(values) < 24:
        values.insert(0, values[0] if values else 20.0)

    predictions = []
    base_time = datetime.now(timezone.utc)

    for step in range(HORIZON):
        lags = values[-24:][::-1]  # lag1=values[-1], lag2=values[-2], ...
        features = {f"pm2_5_lag{i+1}": lags[i] for i in range(24)}
        features["hour"] = (base_time + timedelta(hours=step + 1)).hour
        features["dow"] = (base_time + timedelta(hours=step + 1)).weekday()
        features["temperature"] = 28.0
        features["humidity"] = 70.0
        features["wind_speed"] = 5.0

        row = pd.DataFrame([features])[feature_cols] if feature_cols else pd.DataFrame([features])
        pred = float(model.predict(row)[0])
        horizon_at = (base_time + timedelta(hours=step + 1)).isoformat().replace("+00:00", "Z")

        predictions.append(Prediction(
            horizonAt=horizon_at,
            predicted=round(pred, 2),
            lowerBound=round(max(0, pred - 1.96 * residual_std), 2),
            upperBound=round(pred + 1.96 * residual_std, 2),
        ))
        values.append(pred)

    return ForecastResponse(location=request.location, model=model_name, predictions=predictions)
