package com.app.auth. exception;

import com.app.auth. model.TechnicianProfile;

public class TechnicianNotApprovedException extends AuthException {

    private final TechnicianProfile. ApprovalStatus status;
    private final String rejectionReason;

    public TechnicianNotApprovedException(String message, TechnicianProfile.ApprovalStatus status) {
        super(message);
        this.status = status;
        this.rejectionReason = null;
    }

    public TechnicianNotApprovedException(String message, TechnicianProfile. ApprovalStatus status, String rejectionReason) {
        super(message);
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public TechnicianProfile.ApprovalStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}