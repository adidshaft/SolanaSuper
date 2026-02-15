package com.solanasuper.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<OfflineTransaction> __insertionAdapterOfOfflineTransaction;

  private final Converters __converters = new Converters();

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
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
  }

  @Override
  public Object insert(final OfflineTransaction transaction,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfOfflineTransaction.insertAndReturnId(transaction);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllTransactions(
      final Continuation<? super List<OfflineTransaction>> $completion) {
    final String _sql = "SELECT * FROM offline_transactions ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<OfflineTransaction>>() {
      @Override
      @NonNull
      public List<OfflineTransaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRecipientId = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientId");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfProof = CursorUtil.getColumnIndexOrThrow(_cursor, "proof");
          final List<OfflineTransaction> _result = new ArrayList<OfflineTransaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final OfflineTransaction _item;
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
            _item = new OfflineTransaction(_tmpId,_tmpAmount,_tmpTimestamp,_tmpRecipientId,_tmpStatus,_tmpProof);
            _result.add(_item);
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
  public Object getAvailableBalance(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT SUM(amount) FROM offline_transactions WHERE status = 'AVAILABLE'";
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
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
