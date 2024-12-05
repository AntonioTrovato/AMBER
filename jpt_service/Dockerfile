FROM python:3.9-bookworm

WORKDIR /service

COPY . /service

RUN pip install --no-cache-dir -r requirements.txt

EXPOSE 5001

CMD ["python", "service.py"]