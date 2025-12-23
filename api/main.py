import os
import logging
from typing import List

from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from sqlalchemy import create_engine, Column, Integer, String, Numeric
from sqlalchemy.orm import sessionmaker, declarative_base, Session

app = FastAPI()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger("financy_api")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- DB config ---
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    DATABASE_URL = "postgresql+psycopg2://financy:financy_pass_123@localhost:5432/financy_db"

engine = create_engine(DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)

Base = declarative_base()

class Operation(Base):
    __tablename__ = "operations"

    id = Column(Integer, primary_key=True, index=True)
    type = Column(String, nullable=False)
    amount = Column(Numeric(12, 2), nullable=False)
    category = Column(String, nullable=False)
    date = Column(String, nullable=False)
    note = Column(String, nullable=False)

Base.metadata.create_all(bind=engine)

def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# --- Schemas ---
class OperationCreate(BaseModel):
    type: str
    amount: float
    category: str
    date: str
    note: str

class OperationUpdate(BaseModel):
    type: str
    amount: float
    category: str
    date: str
    note: str

class OperationResponse(BaseModel):
    id: int
    type: str
    amount: float
    category: str
    date: str
    note: str

    class Config:
        from_attributes = True  # pydantic v2

# --- Routes ---
@app.get("/operations", response_model=List[OperationResponse])
def get_operations(db: Session = Depends(get_db)):
    ops = db.query(Operation).order_by(Operation.id.asc()).all()
    logger.info("GET /operations count=%d", len(ops))
    return ops

@app.post("/operations", response_model=OperationResponse)
def create_operation(operation: OperationCreate, db: Session = Depends(get_db)):
    logger.info("POST /operations payload=%s", operation.model_dump_json())
    op = Operation(**operation.model_dump())
    db.add(op)
    db.commit()
    db.refresh(op)
    logger.info("Created operation id=%d", op.id)
    return op

@app.get("/operations/{operation_id}", response_model=OperationResponse)
def get_operation(operation_id: int, db: Session = Depends(get_db)):
    logger.info("GET /operations/%s", operation_id)
    op = db.query(Operation).filter(Operation.id == operation_id).first()
    if not op:
        raise HTTPException(status_code=404, detail="Operation not found")
    return op

@app.put("/operations/{operation_id}", response_model=OperationResponse)
def update_operation(operation_id: int, operation: OperationUpdate, db: Session = Depends(get_db)):
    logger.info("PUT /operations/%s payload=%s", operation_id, operation.model_dump_json())
    op = db.query(Operation).filter(Operation.id == operation_id).first()
    if not op:
        raise HTTPException(status_code=404, detail="Operation not found")

    data = operation.model_dump()
    op.type = data["type"]
    op.amount = data["amount"]
    op.category = data["category"]
    op.date = data["date"]
    op.note = data["note"]

    db.commit()
    db.refresh(op)
    logger.info("Updated operation id=%s", operation_id)
    return op

@app.delete("/operations/{operation_id}")
def delete_operation(operation_id: int, db: Session = Depends(get_db)):
    logger.info("DELETE /operations/%s", operation_id)
    op = db.query(Operation).filter(Operation.id == operation_id).first()
    if not op:
        raise HTTPException(status_code=404, detail="Operation not found")

    db.delete(op)
    db.commit()
    logger.info("Deleted operation id=%s", operation_id)
    return {"message": "Operation deleted"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
