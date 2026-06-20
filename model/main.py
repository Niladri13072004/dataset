import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional
import joblib
import pandas as pd
import numpy as np

app = FastAPI(title="ASTRAM Traffic Predictive Engine ML API")

# 1. Match the Spring Boot REST client input schema
class EventAnalysisRequest(BaseModel):
    type: str
    location: str
    attendance: int
    duration: str
    startDate: str
    notes: Optional[str] = None
    latitude: float
    longitude: float

# 2. Load the uploaded pipeline assets on startup
try:
    priority_model = joblib.load("model_priority.pkl")
    closure_model = joblib.load("model_closure.pkl")
    label_encoders = joblib.load("label_encoders.pkl")  # Contains cause, type, corridor, veh_type encoders
    le_priority = joblib.load("le_priority.pkl")          # Priority target label encoder
    print("SUCCESS: All tracking model .pkl files successfully connected.")
except Exception as e:
    print("CRITICAL: Error loading pickle assets. Fallback rules active.", e)
    priority_model = None

def get_encoded_value(encoder_key, text_value, fallback_index=0):
    """Safely extracts label encoding maps, handling unseen frontend categories gracefully."""
    if encoder_key in label_encoders:
        le = label_encoders[encoder_key]
        cleaned_val = str(text_value).strip()
        # Handle case variations to match encoder training state
        for c in le.classes_:
            if c.lower() == cleaned_val.lower():
                return int(le.transform([c])[0])
        # If not explicit, try exact match
        if cleaned_val in le.classes_:
            return int(le.transform([cleaned_val])[0])
    return fallback_index

@app.post("/analyze")
def analyze_event(request: EventAnalysisRequest):
    # Fallback default values
    priority_prediction = "Low"
    road_closure_prediction = "No"

    if priority_model and closure_model:
        try:
            # Map frontend types to training set targets ('festival' -> 'public_event', etc.)
            fe_type = request.type.lower()
            mapped_cause = "public_event"
            if "protest" in fe_type or "political" in fe_type:
                mapped_cause = "protest"
            elif "construction" in fe_type or "road" in fe_type:
                mapped_cause = "construction"
            elif "procession" in fe_type:
                mapped_cause = "procession"

            # Determine macro event classification state
            mapped_type = "planned" if fe_type in ["festival", "concert", "sports", "political", "planned"] else "unplanned"

            # Detect main corridors from location text, default to Non-corridor fallback index
            loc_lower = request.location.lower()
            mapped_corridor = "Non-corridor"
            if "mg road" in loc_lower or "brigade" in loc_lower or "residency" in loc_lower:
                mapped_corridor = "CBD 1"
            elif "hosur" in loc_lower:
                mapped_corridor = "Hosur Road"
            elif "bannerghatta" in loc_lower:
                mapped_corridor = "Bannerghata Road"

            # Transform raw textual targets to encoded integers using your loaded encoders
            event_cause_enc = get_encoded_value("event_cause", mapped_cause, fallback_index=10) # 'public_event' baseline
            event_type_enc = get_encoded_value("event_type", mapped_type, fallback_index=0)
            corridor_enc = get_encoded_value("corridor", mapped_corridor, fallback_index=11)     # 'Non-corridor' index
            veh_type_enc = get_encoded_value("veh_type", "others", fallback_index=6)             # standard vehicular mass default

            # Assemble DataFrame matching exact column ordering expected by the models
            input_df = pd.DataFrame([{
                'event_cause_enc': event_cause_enc,
                'event_type_enc': event_type_enc,
                'corridor_enc': corridor_enc,
                'veh_type_enc': veh_type_enc,
                'latitude': request.latitude,
                'longitude': request.longitude
            }])

            # Run inference directly through the connected models
            raw_priority_pred = priority_model.predict(input_df)[0]
            raw_closure_pred = closure_model.predict(input_df)[0]

            # Decode priority class array cleanly
            if hasattr(le_priority, "inverse_transform"):
                priority_prediction = str(le_priority.inverse_transform([raw_priority_pred])[0])
            else:
                priority_prediction = "High" if raw_priority_pred == 0 else "Low"

            # Check binary closure assignment output arrays
            if str(raw_closure_pred).lower() in ['1', 'true', 'yes']:
                road_closure_prediction = "Yes"
            else:
                road_closure_prediction = "No"

        except Exception as inference_err:
            print("Inference error occurred, using rule fallbacks:", inference_err)
            priority_prediction, road_closure_prediction = run_heuristic_fallback(request.attendance, request.type.lower())
    else:
        priority_prediction, road_closure_prediction = run_heuristic_fallback(request.attendance, request.type.lower())

    return {
        "status": "success",
        "priority": priority_prediction,
        "roadClosureRequired": road_closure_prediction
    }

def run_heuristic_fallback(attendance, event_type):
    if attendance > 40000 or event_type in ["political", "concert", "sports"]:
        return "High", "Yes"
    elif attendance > 15000:
        return "Medium", "No"
    return "Low", "No"

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)