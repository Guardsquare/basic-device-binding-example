from flask import Flask, request, jsonify
import string
import random
import ecdsa
import json
import base64
from werkzeug.http import HTTP_STATUS_CODES
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.exceptions import InvalidSignature
from models.login_request import LoginRequest
from models.signed_request import SignedRequest
from models.transfer_request import TransferRequest

HTTP_200_OK = 200
HTTP_400_BAD_REQUEST = 400
HTTP_401_UNAUTHORIZED = 401
HTTP_403_FORBIDDEN = 403

app = Flask(__name__)

TOKEN_LEN = 64

class User:
    def __init__(self, email:str, password:str):
        self.email = email
        self.password = password
        self.balance = 1000 # $1000
        self.session_token = None
        self.binding_pub_key = None
        


users = [User('Joe', 'pass123'), User('Jack', 'pass123')]
active_sessions = {}


def verify_signature(public_key_bytes_base64, signature_base64, message):
    
    public_key_bytes_der = base64.b64decode(public_key_bytes_base64)
    public_key_bytes = serialization.load_der_public_key(public_key_bytes_der)
    signature = base64.b64decode(signature_base64)
    try:
        public_key_bytes.verify(
            signature,
            message.encode(),
            ec.ECDSA(hashes.SHA256())
        )
        return True
    except InvalidSignature:
        print("Warning! The message was tampered!!!")
        return False

def find_user(login_request : LoginRequest):
    result = list(filter(lambda user: user.email == login_request.email and user.password == login_request.password, users))
    if len(result) == 0:
        return None
    return result[0]

def get_user_for_session(request):
    return active_sessions.get(request.headers['Authorization'])
        
def extract_original_payload(request_body):
    signed_payload = json.loads(request.json["signedPayload"])
    return signed_payload
    original_payload = originalPayload

@app.route('/loginWithHandshake', methods=['POST'])
def login_with_handshake():
    print(f"\n/loginWithHandshake ==> {str(request.json)}")
    
    # Parse the signed request
    signed_request = SignedRequest(**request.json)
    
    # Extract the original body of the request
    login_request = LoginRequest(**signed_request.get_json_body())
    
    # Verify the signature even though the pubkey comes from this request.
    # It ensures that this request and all subsequent requests are signed 
    # with the same private key
    if verify_signature(login_request.pubkey, signed_request.signature, signed_request.signedPayload) is False:
        return jsonify(message="Signature verification error"), HTTP_401_UNAUTHORIZED

    user = find_user(login_request)
    if user is None:
        return jsonify(message="There is no such user with this username and password"), HTTP_401_UNAUTHORIZED
    
    user.session_token = create_token(TOKEN_LEN)
    user.binding_pub_key = login_request.pubkey
    active_sessions[user.session_token] = user
    print(f"Session Token = {user.session_token}")
    return jsonify(token=user.session_token)


@app.route('/loginWithoutHandshake', methods=['POST'])
def login_without_handshake():
    print(f"\n/loginWithoutHandshake ==> {str(request.json)}")
    
    # Parse the signed request
    signed_request = SignedRequest(**request.json)
    
    # Extract the original body of the request
    login_request = LoginRequest(**signed_request.get_json_body())
    
    user = find_user(login_request)
    if user is None:
        return jsonify(message="There is no such user with this username and password"), HTTP_401_UNAUTHORIZED
    
    if user.binding_pub_key == None:
        return jsonify(message="This user is not bound to this device. Invoke the handshake again"), HTTP_403_FORBIDDEN
    
    if verify_signature(user.binding_pub_key, signed_request.signature, signed_request.signedPayload) is False:
        return jsonify(message="Signature verification error"), HTTP_403_FORBIDDEN

    user.session_token = create_token(TOKEN_LEN)
    active_sessions[user.session_token] = user
    print(f"Session Token = {user.session_token}")
    return jsonify(token=user.session_token)


@app.route('/transfer', methods=['POST'])
def transfer_request():
    print(f"/transfer ==> {str(request.json)}")
    
    # Parse the signed request
    signed_request = SignedRequest(**request.json)
    
    # Extract the original body of the request
    user = get_user_for_session(request)
    if user is None:
        return jsonify(message="Your token is suspicious"), HTTP_401_UNAUTHORIZED
    
    if verify_signature(user.binding_pub_key, signed_request.signature, signed_request.signedPayload) is False:
        return jsonify(message="Signature verification error"), HTTP_403_FORBIDDEN
    
    transfer = TransferRequest(**signed_request.get_json_body())
    # For demo pourpose we only subtract the amount
    user.balance -= transfer.amount
    return jsonify(balance=user.balance)


@app.route('/getBalance', methods=['POST'])
def get_balance_request():
    print(f"/get_balance_request ==> {str(request.json)}")
    
    # Parse the signed request
    signed_request = SignedRequest(**request.json)
    
    # Extract the original body of the request
    user = get_user_for_session(request)
    if user is None:
        return jsonify(message="Your token is suspicious"), HTTP_401_UNAUTHORIZED
    
    if verify_signature(user.binding_pub_key, signed_request.signature, signed_request.signedPayload) is False:
        return jsonify(message="Signature verification error"), HTTP_403_FORBIDDEN
    
    return jsonify(balance=user.balance)
    

def create_token(length):
    return ''.join(random.choice(string.ascii_letters) for i in range(length))
    

if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0", port=8282)
