import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List

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

operations = []
next_id = 1

class OperationCreate(BaseModel):
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

class OperationUpdate(BaseModel):
    type: str
    amount: float
    category: str
    date: str
    note: str

@app.get("/operations", response_model=List[OperationResponse])
def get_operations():
    logger.info("GET /operations count=%d", len(operations))
    return operations

@app.post("/operations", response_model=OperationResponse)
def create_operation(operation: OperationCreate):
    global next_id
    logger.info("POST /operations payload=%s", operation.json())
    new_operation = OperationResponse(
        id=next_id,
        type=operation.type,
        amount=operation.amount,
        category=operation.category,
        date=operation.date,
        note=operation.note
    )
    operations.append(new_operation)
    next_id += 1
    logger.info("Created operation id=%d total=%d", new_operation.id, len(operations))
    return new_operation

@app.get("/operations/{operation_id}", response_model=OperationResponse)
def get_operation(operation_id: int):
    logger.info("GET /operations/%s", operation_id)
    for op in operations:
        if op.id == operation_id:
            return op
    raise HTTPException(status_code=404, detail="Operation not found")

@app.put("/operations/{operation_id}", response_model=OperationResponse)
def update_operation(operation_id: int, operation: OperationUpdate):
    logger.info("PUT /operations/%s payload=%s", operation_id, operation.json())
    for i, op in enumerate(operations):
        if op.id == operation_id:
            updated_operation = OperationResponse(
                id=operation_id,
                type=operation.type,
                amount=operation.amount,
                category=operation.category,
                date=operation.date,
                note=operation.note
            )
            operations[i] = updated_operation
            logger.info("Updated operation id=%s", operation_id)
            return updated_operation
    raise HTTPException(status_code=404, detail="Operation not found")

@app.delete("/operations/{operation_id}")
def delete_operation(operation_id: int):
    logger.info("DELETE /operations/%s", operation_id)
    for i, op in enumerate(operations):
        if op.id == operation_id:
            operations.pop(i)
            logger.info("Deleted operation id=%s remaining=%d", operation_id, len(operations))
            return {"message": "Operation deleted"}
    raise HTTPException(status_code=404, detail="Operation not found")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)

