package com.solanasuper.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HealthDao_Impl implements HealthDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HealthEntity> __insertionAdapterOfHealthEntity;

  public HealthDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHealthEntity = new EntityInsertionAdapter<HealthEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `health_records` (`id`,`timestamp`,`data_type`,`encrypted_payload`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getDataType());
        statement.bindString(4, entity.getEncryptedPayload());
      }
    };
  }

  @Override
  public void insertHealthRecord(final HealthEntity record) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfHealthEntity.insert(record);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public HealthEntity getHealthRecord(final String id) {
    final String _sql = "SELECT * FROM health_records WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfDataType = CursorUtil.getColumnIndexOrThrow(_cursor, "data_type");
      final int _cursorIndexOfEncryptedPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "encrypted_payload");
      final HealthEntity _result;
      if (_cursor.moveToFirst()) {
        final String _tmpId;
        _tmpId = _cursor.getString(_cursorIndexOfId);
        final long _tmpTimestamp;
        _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        final String _tmpDataType;
        _tmpDataType = _cursor.getString(_cursorIndexOfDataType);
        final String _tmpEncryptedPayload;
        _tmpEncryptedPayload = _cursor.getString(_cursorIndexOfEncryptedPayload);
        _result = new HealthEntity(_tmpId,_tmpTimestamp,_tmpDataType,_tmpEncryptedPayload);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<HealthEntity> getAllHealthRecords() {
    final String _sql = "SELECT * FROM health_records";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfDataType = CursorUtil.getColumnIndexOrThrow(_cursor, "data_type");
      final int _cursorIndexOfEncryptedPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "encrypted_payload");
      final List<HealthEntity> _result = new ArrayList<HealthEntity>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final HealthEntity _item;
        final String _tmpId;
        _tmpId = _cursor.getString(_cursorIndexOfId);
        final long _tmpTimestamp;
        _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        final String _tmpDataType;
        _tmpDataType = _cursor.getString(_cursorIndexOfDataType);
        final String _tmpEncryptedPayload;
        _tmpEncryptedPayload = _cursor.getString(_cursorIndexOfEncryptedPayload);
        _item = new HealthEntity(_tmpId,_tmpTimestamp,_tmpDataType,_tmpEncryptedPayload);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
