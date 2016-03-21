package ru.ncapital.gateways.micexfast.connection.messageprocessors;

import ru.ncapital.gateways.micexfast.Utils;

public class SequenceArray {

    private static final int SEQUENCE_ARRAY_SIZE = 100;

    private static final int SEQUENCE_ARRAY_SIZE_HALF = 50;

    private int[] iVector;

    private int iPosition;

    private int iNewPosition;

    public enum Result {
        OUT_OF_SEQUENCE(-1), DUPLICATE(0), IN_SEQUENCE(1);

        private int iValue;

        Result(int value) {
            iValue = value;
        }

        int value() {
            return iValue;
        }
    }

    public SequenceArray() {
        iPosition = 0;
        iNewPosition = 0;
        iVector = new int[SEQUENCE_ARRAY_SIZE];
        for (int i = 0; i < SEQUENCE_ARRAY_SIZE; ++i)
            iVector[i] = 0;
    }

    public synchronized void clear() {
        for (int i = 0; i < SEQUENCE_ARRAY_SIZE; ++i)
            iVector[i] = 0;

        iPosition = 0;
        iNewPosition = 0;
    }

    public synchronized Result checkDuplicate(final int value) {
        switch (findPosition(value)) {
            case 1:
                return checkInSequence() ? Result.IN_SEQUENCE : Result.OUT_OF_SEQUENCE;
            case -1:
                return Result.OUT_OF_SEQUENCE;
            case 0:
            default:
                return Result.DUPLICATE;
        }
    }

    public synchronized Result checkSequence(final int value) {
        switch (findPosition(value)) {
            case 1:
                insertEntryUp(value);
                return checkInSequence() ? Result.IN_SEQUENCE : Result.OUT_OF_SEQUENCE;
            case -1:
                insertEntryDown(value);
                return Result.OUT_OF_SEQUENCE;
            case 0:
            default:
                return Result.DUPLICATE;
        }
    }

    private void insertEntryDown(int value) {
        int i = (iPosition + 1 == SEQUENCE_ARRAY_SIZE) ? 0 : (iPosition + 1);

        for (; i < SEQUENCE_ARRAY_SIZE && i != (iNewPosition != 0 ? (iNewPosition - 1) : (SEQUENCE_ARRAY_SIZE - 1)); ) {
            iVector[i] = iVector[((i + 1) >= SEQUENCE_ARRAY_SIZE) ? 0 : (i + 1)];
            if (i < SEQUENCE_ARRAY_SIZE - 1)
                ++i;
            else
                i = 0;
        }

        iVector[((iNewPosition > 0) ? iNewPosition : SEQUENCE_ARRAY_SIZE) - 1] = value;
    }

    private void insertEntryUp(int value) {
        int i = (iPosition + 1 == SEQUENCE_ARRAY_SIZE) ? 0 : (iPosition + 1);

        for (; i != iNewPosition; ) {
            iVector[i] = iVector[((i - 1) < 0) ? (SEQUENCE_ARRAY_SIZE - 1) : (i - 1)];
            if (i > 0)
                --i;
            else
                i = SEQUENCE_ARRAY_SIZE - 1;
        }

        iVector[iNewPosition] = value;
        iPosition = (((iPosition + 1) < SEQUENCE_ARRAY_SIZE) ? iPosition + 1 : 0);
    }

    private int findPosition(int value) {
        int j = 0;
        iNewPosition = 0;

        if (iVector[iPosition] < value) { // greater value
            iNewPosition = (iPosition + 1 == SEQUENCE_ARRAY_SIZE) ? 0 : (iPosition + 1);
            return 1;
        }

        if (iVector[((iPosition > 0) ? iPosition : SEQUENCE_ARRAY_SIZE) - 1] == value) { // duplicate value
            return 0;
        }

        for (int i = iPosition; i >= 0; --i, ++j) {
            if (iVector[i] < value) {
                iNewPosition = (i + 1 == SEQUENCE_ARRAY_SIZE) ? 0 : (i + 1);
                return (j < SEQUENCE_ARRAY_SIZE_HALF) ? 1 : -1;
            }

            if (iVector[i] == value)
                return 0;

            if (iVector[i] > value)
                continue;
        }

        for (int i = SEQUENCE_ARRAY_SIZE - 1; i > iPosition; --i, ++j) {
            if (iVector[i] < value) {
                iNewPosition = (i + 1 == SEQUENCE_ARRAY_SIZE) ? 0 : (i + 1);
                return (j < SEQUENCE_ARRAY_SIZE_HALF) ? 1 : -1;
            }

            if (iVector[i] == value)
                return 0;

            if (iVector[i] > value)
                continue;
        }

        reset();
        return findPosition(value);
    }

    private boolean checkInSequence() {
        return iVector[iNewPosition] - 1 == iVector[((iNewPosition > 0) ? iNewPosition : SEQUENCE_ARRAY_SIZE) - 1];
    }

    private void reset() {
        Utils.updateTodayInMillis();

        iNewPosition = 0;
        iPosition = 0;
        for (int i = 0; i < iVector.length; ++i)
            iVector[i] = 0;
    }
}
