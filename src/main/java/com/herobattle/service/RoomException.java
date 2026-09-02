package com.herobattle.service;

/** Base type for room-related failures surfaced to REST clients. */
public abstract class RoomException extends RuntimeException {

    protected RoomException(String message) {
        super(message);
    }

    /** Room code does not exist. Maps to HTTP 404. */
    public static class RoomNotFound extends RoomException {
        public RoomNotFound(String code) {
            super("Room not found: " + code);
        }
    }

    /** Room exists but cannot be joined (already started, finished, or full). Maps to HTTP 409. */
    public static class RoomNotJoinable extends RoomException {
        public RoomNotJoinable(String message) {
            super(message);
        }
    }
}
