from flask import Flask, request, Response
import os
from minio import Minio
from io import BytesIO

app = Flask(__name__)

endpoint = os.environ["MINIO_ENDPOINT"]
secure = endpoint.startswith("https://")
host = endpoint.replace("http://", "").replace("https://", "")

client = Minio(
    host,
    access_key=os.environ["MINIO_ACCESS_KEY"],
    secret_key=os.environ["MINIO_SECRET_KEY"],
    secure=secure,
)

bucket = os.environ.get("MINIO_BUCKET", "src-bucket")
obj = os.environ.get("MINIO_OBJECT", "test.csv")

@app.get("/health")
def health():
    return "ok\n"

@app.post("/upload")
def upload():
    data = request.get_data()
    if data is None:
        return Response("no body\n", status=400)

    client.put_object(
        bucket,
        obj,
        BytesIO(data),
        length=len(data),
        content_type=request.content_type or "application/octet-stream",
    )
    return "ok\n"

app.run(host="0.0.0.0", port=7070)
