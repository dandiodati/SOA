package com.nightfire.adapter.util;

/**
 * Copyright (c) 2004 NeuStar, Inc. All rights reserved.
 * $Header: $
 */

import com.nightfire.framework.util.*;

public class ColumnMetaData {

  public final String schemaName;
  public final String columnName;
  public final int type;
  public final String typeName;
  public final int size;
  public final int decimalDigits;
  public final String defaultValue;
  public final int ordPos;
  public final boolean isNull;

  public ColumnMetaData(String schemaName,
                        String colName,
                        int type,
                        String typeName,
                        int size, int dd,
                        String dv,
                        int ordPos,
                        String isNull) {
    this.columnName = colName;
    this.type = type;
    this.typeName = typeName;
    this.schemaName = schemaName;
    this.size = size;
    this.decimalDigits = dd;
    this.defaultValue = stripQuotes(dv);
    this.ordPos = ordPos;
    this.isNull = isNull.equalsIgnoreCase("YES") ? true : false;
  }

  public String describe() {
    StringBuffer sb = new StringBuffer();

    sb.append("Column description: Name [");
    sb.append(columnName);

    if (StringUtils.hasValue(columnName)) {
      sb.append("], column [");
      sb.append(columnName);
    }

    if (StringUtils.hasValue(typeName)) {
      sb.append("], type [");
      sb.append(typeName);
    }

    if (StringUtils.hasValue(defaultValue)) {
      sb.append("], default [");
      sb.append(defaultValue);
    }

    sb.append("].");

    return (sb.toString());
  }

  private static String stripQuotes(String str) {
    if (str == null) {
      return null;
    }
    char[] st = str.toCharArray();
    StringBuffer sb = new StringBuffer(str.length());
    int len = str.length();
    for (int i = 0; i < len; i++) {
      if (st[i] != '\'') {
        sb.append(st[i]);
      }
    }
    return sb.toString();
  }

}