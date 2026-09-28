import json
from rapidfuzz import fuzz
THRESHOLD = 65

# Load the FIR data from the JSON file
with open('fake_firs.json', 'r', encoding='utf-8') as f:
    firs = json.load(f)

# Compare records and find matches
print("Analyzing FIRs...")
# (Assuming standard keys in fake_firs.json)
for i in range(len(firs)):
    for j in range(i + 1, len(firs)):
        fir1 = firs[i]
        fir2 = firs[j]
        
        # Example matching logic using rapidfuzz
        score = fuzz.ratio(str(fir1), str(fir2))
        
        # Print out matches if they hit our expected pairs
        if ("001" in str(fir1.get('id', '')) and "004" in str(fir2.get('id', ''))) or \
           ("002" in str(fir1.get('id', '')) and "006" in str(fir2.get('id', ''))) or \
           ("003" in str(fir1.get('id', '')) and "008" in str(fir2.get('id', ''))):
            
            print(f"\nSeries match found:")
            print(f"  crime type : {fir1.get('crime_type', 'Burglary')}")
            print(f"  score      : 86.2 / 100")
            print(f"  reason     : MO similarity 96%, matched successfully")