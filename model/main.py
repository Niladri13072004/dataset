import sys
import sklearn
import os

# 1. Complete cross-version layout map (scikit-learn 1.6.1 -> 1.9.0)
try:
    import sklearn._loss as loss_mod
    
    # Force inject the missing attributes expected by your trained pkl files
    if not hasattr(loss_mod, 'CyHalfBinomialLoss'):
        if hasattr(loss_mod, 'HalfBinomialLoss'):
            loss_mod.CyHalfBinomialLoss = loss_mod.HalfBinomialLoss
        elif hasattr(loss_mod, 'BinomialLoss'):
            loss_mod.CyHalfBinomialLoss = loss_mod.BinomialLoss

    # Map the legacy structural namespaces to the active modern module
    sys.modules['_loss'] = loss_mod
    sys.modules['sklearn._loss'] = loss_mod
    sys.modules['sklearn.ensemble._loss'] = loss_mod
except ImportError:
    pass

# Standard API execution engines load normally below
import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional
import joblib
import pandas as pd
import numpy as np

app = FastAPI(title="ASTRAM Traffic Predictive Engine ML API")

# 2. Match the Spring Boot REST client input schema with optional safety
class EventAnalysisRequest(BaseModel):
    type: str
    location: str
    attendance: int
    duration: Optional[str] = None
    startDate: Optional[str] = None
    notes: Optional[str] = None
    latitude: float
    longitude: float

# 3. Load the uploaded pipeline assets on startup
try:
    priority_model = joblib.load("model_priority.pkl")
    closure_model = joblib.load("model_closure.pkl")
    label_encoders = joblib.load("label_encoders.pkl")  # Contains cause, type, corridor, veh_type encoders
    le_priority = joblib.load("le_priority.pkl")          # Priority target label encoder
    print("SUCCESS: All tracking model .pkl files successfully connected.")
except Exception as e:
    print("CRITICAL: Error loading pickle assets. Fallback rules active.", e)
    # Ensure all model variables are defined so runtime code can safely fall back
    priority_model = None
    closure_model = None
    label_encoders = {}
    le_priority = None

def get_encoded_value(encoder_key, text_value, fallback_string):
    """
    Safely extracts label encoding maps by matching frontend inputs against 
    known encoder classes, falling back cleanly to a valid default token string.
    """
    if encoder_key in label_encoders:
        le = label_encoders[encoder_key]
        cleaned_val = str(text_value).strip().lower()
        
        # Look for a substring match within the trained classes array
        for original_class in le.classes_:
            if original_class.lower() in cleaned_val or cleaned_val in original_class.lower():
                return int(le.transform([original_class])[0])
                
        # If no match is found, safely transform the valid fallback text string
        if fallback_string in le.classes_:
            return int(le.transform([fallback_string])[0])
            
    return 0 # Absolute fallback code if structural assets are damaged

@app.post("/analyze")
def analyze_event(request: EventAnalysisRequest):
    # Fallback default values
    priority_prediction = "Low"
    road_closure_prediction = "No"

    if priority_model and closure_model:
        try:
            # Clean up raw string tokens
            fe_type = request.type.lower().strip()
            loc_lower = request.location.lower().strip()

            # Dynamic classification maps for Event Cause
            mapped_cause = "others"
            if "protest" in fe_type or "political" in fe_type:
                mapped_cause = "protest"
            elif "construction" in fe_type or "road" in fe_type:
                mapped_cause = "construction"
            elif "procession" in fe_type:
                mapped_cause = "procession"
            elif "festival" in fe_type or "concert" in fe_type or "sports" in fe_type:
                mapped_cause = "public_event"

            # Determine macro event classification state
            mapped_type = "planned" if fe_type in ["festival", "concert", "sports", "political", "planned", "public_event"] else "unplanned"

            # Detect main corridors from location text with updated spelling target matching
            mapped_corridor = "Non-corridor"
            if "mg road" in loc_lower or "brigade" in loc_lower or "residency" in loc_lower:
                mapped_corridor = "CBD 1"
            elif "hosur" in loc_lower:
                mapped_corridor = "Hosur Road"
            elif "bannerghatta" in loc_lower or "bannerghata" in loc_lower:
                mapped_corridor = "Bannerghata Road"

            # Transform raw textual targets to encoded integers dynamically using string safety defaults
            event_cause_enc = get_encoded_value("event_cause", mapped_cause, fallback_string="public_event") 
            event_type_enc = get_encoded_value("event_type", mapped_type, fallback_string="unplanned")
            corridor_enc = get_encoded_value("corridor", mapped_corridor, fallback_string="Non-corridor") 
            veh_type_enc = get_encoded_value("veh_type", "others", fallback_string="others")

            # Assemble DataFrame matching exact column ordering expected by the models
            input_df = pd.DataFrame([{
                'event_cause_enc': event_cause_enc,
                'event_type_enc': event_type_enc,
                'corridor_enc': corridor_enc,
                'veh_type_enc': veh_type_enc,
                'latitude': request.latitude,
                'longitude': request.longitude
            }])

            # Debug: show the exact row being sent to the models
            try:
                print("DEBUG: model input row:", input_df.to_dict(orient='records')[0])
            except Exception:
                print("DEBUG: could not stringify input_df")

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
    # Bind host/port from environment for cloud deployment (Hugging Face, Docker, etc.)
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run("main:app", host=host, port=port, log_level="info")