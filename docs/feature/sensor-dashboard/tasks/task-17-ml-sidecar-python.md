# Task 17: ML Sidecar (Python FastAPI)

**Status:** pending
**HLD Reference:** §Technical Implementation — ML Forecast Service (Python ML sidecar), §Milestones M6, §ML Approach §9.1

## Description

Build the Python FastAPI ML sidecar that trains a LightGBM model on historical PM2.5 data with weather exogenous features, serializes it, and serves predictions via a `/forecast` POST endpoint. The training script runs at Docker image build time (or can be re-run to retrain). The service also exposes a `/metrics` endpoint reporting MAE/RMSE vs a persistence naive baseline on the held-out temporal test split.

## Acceptance Criteria

- [ ] `ml-service/` directory with `Dockerfile`, `requirements.txt`, `train.py`, `app.py`
- [ ] `train.py`: loads historical Open-Meteo data (or synthetic fixture), trains LightGBM with features: `[pm2_5_lag1, pm2_5_lag2, ..., pm2_5_lag24, wind_speed, humidity, temperature, hour_of_day, day_of_week]`, uses temporal train/test split (last 20% for test), evaluates MAE/RMSE at each horizon step, saves model as `model.pkl` and metrics as `metrics.json`
- [ ] `app.py` FastAPI: `POST /forecast` accepts `{ "readings": [...], "features": {...} }`, returns `{ "predicted": [...], "lower": [...], "upper": [...], "model": "lgbm", "horizon_steps": [...] }`
- [ ] `GET /metrics` returns content of `metrics.json` (MAE/RMSE per horizon step + baseline comparison)
- [ ] `GET /health` returns `{"status":"ok"}` (used by Docker healthcheck)
- [ ] Confidence band: `lower = predicted - 1.96 * residual_std`, `upper = predicted + 1.96 * residual_std` (approximate 95% interval from training residuals)
- [ ] ARM64-compatible: uses `lightgbm` package that installs on Apple Silicon; if not, falls back to `scikit-learn` gradient boosting (document in README)
- [ ] `docker build ml-service/` succeeds with no errors

## Dependencies

- **Depends on:** Task 04 (domain contract for request/response shape)
- **Blocks:** Task 18 (Spring ForecastClient calls this service)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `ml-service/Dockerfile` | Create | Python image with model pre-trained |
| `ml-service/requirements.txt` | Create | `fastapi`, `uvicorn`, `lightgbm`, `scikit-learn`, `pandas`, `numpy`, `joblib` |
| `ml-service/train.py` | Create | Training script: data load, feature eng, temporal split, train, eval, save |
| `ml-service/app.py` | Create | FastAPI app with /forecast, /metrics, /health |
| `ml-service/data/training_fixture.csv` | Create | Synthetic or downloaded historical data for bootstrap training |

## Implementation Hints

- **Temporal split (not random):**
  ```python
  split_idx = int(len(df) * 0.8)
  train, test = df.iloc[:split_idx], df.iloc[split_idx:]
  ```
- **Feature engineering:**
  ```python
  for lag in range(1, 25):
      df[f'pm2_5_lag{lag}'] = df['pm2_5'].shift(lag)
  df['hour'] = df.index.hour
  df['dow'] = df.index.dayofweek
  df = df.dropna()
  ```
- **Forecast endpoint contract:**
  ```json
  POST /forecast
  {
    "readings": [{"observedAt":"...", "value":38.4}, ...],
    "features": {"wind_speed": [3.1, ...], "humidity": [72, ...], "temperature": [28, ...]}
  }
  Response:
  {
    "predicted": [39.1, 40.2, ...],
    "lower":     [35.0, 36.0, ...],
    "upper":     [43.2, 44.4, ...],
    "horizon_steps": ["2026-05-22T04:00Z", ...],
    "model": "lgbm"
  }
  ```
- **Key consideration (§9.1 leakage):** The training data and prediction features both come from Open-Meteo CAMS models — this is circularity. Frame this honestly: the model is a cheap surrogate/now-cast, not an attempt to beat CAMS. The `/metrics` endpoint shows this comparison transparently. Document in README.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
