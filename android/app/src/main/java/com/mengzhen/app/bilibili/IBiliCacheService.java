package com.mengzhen.app.bilibili;

/**
 * Source form of the Binder interface generated from IBiliCacheService.aidl.
 *
 * Keeping the generated source avoids an AIDL/Javac encoding failure caused by
 * non-ASCII Android SDK paths on Windows while preserving the exact IPC contract.
 */
public interface IBiliCacheService extends android.os.IInterface {
    abstract class Stub extends android.os.Binder implements IBiliCacheService {
        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IBiliCacheService asInterface(android.os.IBinder binder) {
            if (binder == null) return null;
            android.os.IInterface local = binder.queryLocalInterface(DESCRIPTOR);
            if (local instanceof IBiliCacheService) return (IBiliCacheService) local;
            return new Proxy(binder);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(
                int code,
                android.os.Parcel data,
                android.os.Parcel reply,
                int flags
        ) throws android.os.RemoteException {
            if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION
                    && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(DESCRIPTOR);
            }
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(DESCRIPTOR);
                return true;
            }
            switch (code) {
                case TRANSACTION_destroy:
                    destroy();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_scanDefaultCaches:
                    String scanResult = scanDefaultCaches();
                    reply.writeNoException();
                    reply.writeString(scanResult);
                    return true;
                case TRANSACTION_openFile:
                    android.os.ParcelFileDescriptor descriptor = openFile(data.readString());
                    reply.writeNoException();
                    writeTypedObject(
                            reply,
                            descriptor,
                            android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
                    );
                    return true;
                case TRANSACTION_watchDefaultCaches:
                    android.os.ParcelFileDescriptor changeStream = watchDefaultCaches();
                    reply.writeNoException();
                    writeTypedObject(
                            reply,
                            changeStream,
                            android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE
                    );
                    return true;
                case TRANSACTION_stopWatchingDefaultCaches:
                    stopWatchingDefaultCaches();
                    reply.writeNoException();
                    return true;
                case TRANSACTION_identity:
                    String identityResult = identity();
                    reply.writeNoException();
                    reply.writeString(identityResult);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static final class Proxy implements IBiliCacheService {
            private final android.os.IBinder remote;

            Proxy(android.os.IBinder remote) {
                this.remote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return remote;
            }

            @Override
            public void destroy() throws android.os.RemoteException {
                transactVoid(TRANSACTION_destroy);
            }

            @Override
            public String scanDefaultCaches() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_scanDefaultCaches, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public android.os.ParcelFileDescriptor openFile(String path)
                    throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(path);
                    remote.transact(TRANSACTION_openFile, data, reply, 0);
                    reply.readException();
                    return readTypedObject(reply, android.os.ParcelFileDescriptor.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public android.os.ParcelFileDescriptor watchDefaultCaches()
                    throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_watchDefaultCaches, data, reply, 0);
                    reply.readException();
                    return readTypedObject(reply, android.os.ParcelFileDescriptor.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void stopWatchingDefaultCaches() throws android.os.RemoteException {
                transactVoid(TRANSACTION_stopWatchingDefaultCaches);
            }

            @Override
            public String identity() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(TRANSACTION_identity, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            private void transactVoid(int transaction) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    remote.transact(transaction, data, reply, 0);
                    reply.readException();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        static final int TRANSACTION_destroy =
                android.os.IBinder.FIRST_CALL_TRANSACTION + 16777114;
        static final int TRANSACTION_scanDefaultCaches =
                android.os.IBinder.FIRST_CALL_TRANSACTION + 1;
        static final int TRANSACTION_openFile =
                android.os.IBinder.FIRST_CALL_TRANSACTION + 2;
        static final int TRANSACTION_identity =
                android.os.IBinder.FIRST_CALL_TRANSACTION + 3;
        static final int TRANSACTION_watchDefaultCaches =
                android.os.IBinder.FIRST_CALL_TRANSACTION + 4;
        static final int TRANSACTION_stopWatchingDefaultCaches =
                android.os.IBinder.FIRST_CALL_TRANSACTION + 5;
    }

    String DESCRIPTOR = "com.mengzhen.app.bilibili.IBiliCacheService";

    void destroy() throws android.os.RemoteException;

    String scanDefaultCaches() throws android.os.RemoteException;

    android.os.ParcelFileDescriptor openFile(String absolutePath)
            throws android.os.RemoteException;

    android.os.ParcelFileDescriptor watchDefaultCaches()
            throws android.os.RemoteException;

    void stopWatchingDefaultCaches() throws android.os.RemoteException;

    String identity() throws android.os.RemoteException;

    static <T> T readTypedObject(
            android.os.Parcel parcel,
            android.os.Parcelable.Creator<T> creator
    ) {
        return parcel.readInt() != 0 ? creator.createFromParcel(parcel) : null;
    }

    static <T extends android.os.Parcelable> void writeTypedObject(
            android.os.Parcel parcel,
            T value,
            int flags
    ) {
        if (value == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(1);
            value.writeToParcel(parcel, flags);
        }
    }
}
