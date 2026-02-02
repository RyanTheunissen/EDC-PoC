from flask import Flask, request, Response
import os
import uuid
from minio import Minio
from io import BytesIO
from werkzeug.utils import secure_filename

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

@app.get("/health")
def health():
    return "ok\n"

def pick_filename():
    if "file" in request.files and request.files["file"].filename:
        return secure_filename(request.files["file"].filename)

    hdr = request.headers.get("X-Filename")
    if hdr:
        return secure_filename(hdr)

    q = request.args.get("filename")
    if q:
        return secure_filename(q)

    return f"upload-{uuid.uuid4().hex}.bin"

@app.post("/upload")
def upload():
    if "file" in request.files and request.files["file"].filename:
        f = request.files["file"]
        data = f.read()
        filename = secure_filename(f.filename)
        content_type = f.content_type or "application/octet-stream"
    else:
        data = request.get_data()
        if not data:
            app.logger.warning("Empty body. Headers=%s", dict(request.headers))
            return Response("empty body\n", status=400)

        filename = pick_filename()
        content_type = request.content_type or "application/octet-stream"

    client.put_object(
        bucket,
        filename,
        BytesIO(data),
        length=len(data),
        content_type=content_type,
    )
    return "ok\n"

app.run(host="0.0.0.0", port=7070)
