class BusinessRuleError extends RuntimeException {
    public BusinessRuleError(String msg) { super(msg); }
}

class VehicleAlreadyParkedError extends BusinessRuleError {
    public VehicleAlreadyParkedError(String msg) { super(msg); }
}

class LotFullError extends BusinessRuleError {
    public LotFullError(String msg) { super(msg); }
}

class VehicleNotParkedError extends BusinessRuleError {
    public VehicleNotParkedError(String msg) { super(msg); }
}

class InvalidVehicleTypeError extends BusinessRuleError {
    public InvalidVehicleTypeError(String msg) { super(msg); }
}

