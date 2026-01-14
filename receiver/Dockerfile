FROM python:3.12-alpine
WORKDIR /usr/src/app

RUN python -m pip install --no-cache-dir flask minio

COPY receiver.py /usr/src/app/receiver.py

EXPOSE 7070
CMD ["python", "/usr/src/app/receiver.py"]