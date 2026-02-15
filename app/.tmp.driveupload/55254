package com.solanasuper.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WalletDao_Impl implements WalletDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WalletBalance> __insertionAdapterOfWalletBalance;

  private final EntityInsertionAdapter<OfflineTransaction> __insertionAdapterOfOfflineTransaction;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfUpdateBalance;

  public WalletDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWalletBalance = new EntityInsertionAdapter<WalletBalance>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `wallet_balance` (`id`,`availableBalance`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WalletBalance entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAvailableBalance());
      }
    };
    this.__insertionAdapterOfOfflineTransaction = new EntityInsertionAdapter<OfflineTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `offline_transactions` (`id`,`amount`,`timestamp`,`recipientId`,`status`,`proof`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final OfflineTransaction entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getAmount());
        statement.bindLong(3, entity.getTimestamp());
        if (entity.getRecipientId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getRecipientId());
        }
        final String _tmp = __converters.fromStatus(entity.getStatus());
        statement.bindString(5, _tmp);
        if (entity.getProof() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getProof());
        }
      }
    };
    this.__preparedStmtOfUpdateBalance = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE wallet_balance SET availableBalance = ? WHERE id = 1";
        return _query;
      }
    };
  }

  @Override
  public Object initBalance(final WalletBalance balance,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWalletBalance.insert(balance);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTransaction(final OfflineTransaction transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfOfflineTransaction.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object lockFunds(final String txId, final long amount,
      final Continuation<? super Boolean> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> WalletDao.DefaultImpls.lockFunds(WalletDao_Impl.this, txId, amount, __cont), $completion);
  }

  @Override
  public Object updateBalance(final long newBalance, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBalance.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, newBalance);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateBalance.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAvailableBalance(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT availableBalance FROM wallet_balance WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getLong(0);
            }
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTransaction(final String txId,
      final Continuation<? super OfflineTransaction> $completion) {
    final String _sql = "SELECT * FROM offline_transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, txId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<OfflineTransaction>() {
      @Override
      @Nullable
      public OfflineTransaction call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRecipientId = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProof = CursorUtil.getColumnIndexOrThrow(_cursor, "proof");
          final OfflineTransaction _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmount;
            _tmpAmount = _cursor.getLong(_cursorIndexOfAmount);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpRecipientId;
            if (_cursor.isNull(_cursorIndexOfRecipientId)) {
              _tmpRecipientId = null;
            } else {
              _tmpRecipientId = _cursor.getString(_cursorIndexOfRecipientId);
            }
            final TransactionStatus _tmpStatus;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfStatus);
            _tmpStatus = __converters.toStatus(_tmp);
            final String _tmpProof;
            if (_cursor.isNull(_cursorIndexOfProof)) {
              _tmpProof = null;
            } else {
              _tmpProof = _cursor.getString(_cursorIndexOfProof);
            }
            _result = new OfflineTransaction(_tmpId,_tmpAmount,_tmpTimestamp,_tmpRecipientId,_tmpStatus,_tmpProof);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object initializeBalance(final long amount, final Continuation<? super Unit> $completion) {
    return WalletDao.DefaultImpls.initializeBalance(WalletDao_Impl.this, amount, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
