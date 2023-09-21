class LoginRequest:
    def __init__(self, email, password, pubkey) -> None:
        self.email = email
        self.password = password
        self.pubkey = pubkey # base64(pubkey_DER)
        