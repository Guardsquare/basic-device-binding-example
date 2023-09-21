import json

class SignedRequest:
    def __init__(self, signedPayload : str, signature:str) -> None:
        self.signedPayload = signedPayload
        self.signature = signature

    def extract_signed_payload(self):
        return json.loads(self.signedPayload)
        
    def get_body(self):
        return self.extract_signed_payload()["body"]
    
    def get_json_body(self):
        return json.loads(self.extract_signed_payload()["body"])
        
    def get_session_token(self):
        return  self.extract_signed_payload()["sessionToken"]
