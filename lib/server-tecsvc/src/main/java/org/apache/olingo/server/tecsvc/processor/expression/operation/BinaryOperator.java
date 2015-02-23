/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.server.tecsvc.processor.expression.operation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
import org.apache.olingo.commons.core.edm.primitivetype.EdmByte;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDate;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDouble;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDuration;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt16;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt64;
import org.apache.olingo.commons.core.edm.primitivetype.EdmSByte;
import org.apache.olingo.commons.core.edm.primitivetype.EdmSingle;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.tecsvc.processor.expression.operand.TypedOperand;
import org.apache.olingo.server.tecsvc.processor.expression.operand.VisitorOperand;
import org.apache.olingo.server.tecsvc.processor.expression.primitive.EdmNull;

public class BinaryOperator {
  private static final int FACTOR_SECOND_INT = 1000;
  private static final BigDecimal FACTOR_SECOND = new BigDecimal(1000);
  private static final BigInteger EDM_SBYTE_MIN = BigInteger.valueOf(Byte.MIN_VALUE);
  private static final BigInteger EDN_SBYTE_MAX = BigInteger.valueOf(Byte.MAX_VALUE);
  private static final BigInteger EDM_BYTE_MIN = BigInteger.ZERO;
  private static final BigInteger EDM_BYTE_MAX = BigInteger.valueOf(((Byte.MAX_VALUE * 2) + 1));
  private static final BigInteger EDM_INT16_MIN = BigInteger.valueOf(Short.MIN_VALUE);
  private static final BigInteger EDM_INT16_MAX = BigInteger.valueOf(Short.MAX_VALUE);
  private static final BigInteger EDM_INT32_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
  private static final BigInteger EDM_INT32_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
  private static final BigInteger EDM_INT64_MIN = BigInteger.valueOf(Long.MIN_VALUE);
  private static final BigInteger EDM_INT64_MAX = BigInteger.valueOf(Long.MAX_VALUE);
  private static final BigDecimal EDM_SINGLE_MIN = BigDecimal.valueOf(Float.MIN_VALUE);
  private static final BigDecimal EDM_SINGLE_MAX = BigDecimal.valueOf(Float.MAX_VALUE);

  private static final int EQUALS = 0;
  private static final int LESS_THAN = -1;
  private static final int GREATER_THAN = 1;

  private TypedOperand right;
  private TypedOperand left;

  public BinaryOperator(final VisitorOperand leftOperand, final VisitorOperand rightOperand)
      throws ODataApplicationException {
    left = leftOperand.asTypedOperand();
    right = rightOperand.asTypedOperand();

    left = left.castToCommonType(right);
    right = right.castToCommonType(left);
  }

  public VisitorOperand andOperator() throws ODataApplicationException {
    Boolean result = null;
    if (left.is(EdmBoolean.getInstance()) && right.is(EdmBoolean.getInstance())) {
      if (Boolean.TRUE.equals(left.getValue()) && Boolean.TRUE.equals(right.getValue())) {
        result = true;
      } else if (Boolean.FALSE.equals(left.getValue()) || Boolean.FALSE.equals(right.getValue())) {
        result = false;
      }

      return new TypedOperand(result, EdmBoolean.getInstance());
    } else {
      throw new ODataApplicationException("Add operator needs two binary operands",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
  }

  public VisitorOperand orOperator() throws ODataApplicationException {
    Boolean result = null;
    if (left.is(EdmBoolean.getInstance()) && right.is(EdmBoolean.getInstance())) {
      if (Boolean.TRUE.equals(left.getValue()) || Boolean.TRUE.equals(right.getValue())) {
        result = true;
      } else if (Boolean.FALSE.equals(left.getValue()) && Boolean.FALSE.equals(right.getValue())) {
        result = false;
      }

      return new TypedOperand(result, EdmBoolean.getInstance());
    } else {
      throw new ODataApplicationException("Or operator needs two binary operands",
          HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
    }
  }

  public VisitorOperand equalsOperator() {
    final boolean result = isBinaryComparisonNecessary() && binaryComparison(EQUALS);
    return new TypedOperand(result, EdmBoolean.getInstance());
  }

  public VisitorOperand notEqualsOperator() {
    final VisitorOperand equalsOperator = equalsOperator();
    return new TypedOperand(!(Boolean) equalsOperator.getValue(), EdmBoolean.getInstance());
  }

  private boolean isBinaryComparisonNecessary() {
    // binaryComparison() need to be called, if both operand are either null or not null
    return !(left.isNull() ^ right.isNull());
  }

  public VisitorOperand greaterEqualsOperator() {
    final boolean result = isBinaryComparisonNecessary() && binaryComparison(GREATER_THAN, EQUALS);
    return new TypedOperand(result, EdmBoolean.getInstance());
  }

  public VisitorOperand greaterThanOperator() {
    final boolean result = isBinaryComparisonNecessary() && binaryComparison(GREATER_THAN);
    return new TypedOperand(result, EdmBoolean.getInstance());
  }

  public VisitorOperand lessEqualsOperator() {
    final boolean result = isBinaryComparisonNecessary() && binaryComparison(LESS_THAN, EQUALS);
    return new TypedOperand(result, EdmBoolean.getInstance());
  }

  public VisitorOperand lessThanOperator() {
    final boolean result = isBinaryComparisonNecessary() && binaryComparison(LESS_THAN);
    return new TypedOperand(result, EdmBoolean.getInstance());
  }

  private boolean binaryComparison(int... expect) {
    int result;

    if (left.isNull() && right.isNull()) {
      result = 0; // null is equals to null
    } else {
      // left and right are not null!
      if (left.isIntegerType()) {
        result = left.getTypedValue(BigInteger.class).compareTo(right.getTypedValue(BigInteger.class));
      } else if (left.isDecimalType()) {
        result = left.getTypedValue(BigDecimal.class).compareTo(right.getTypedValue(BigDecimal.class));
      } else {
        result = left.getValue().equals(right.getValue()) ? 0 : 1;
      }
    }
    for (int expectedValue : expect) {
      if (expectedValue == result) {
        return true;
      }
    }

    return false;
  }

  public VisitorOperand arithmeticOperator(BinaryOperatorKind operator) throws ODataApplicationException {
    if (left.isNull() || right.isNull()) {
      return new TypedOperand(new Object(), EdmNull.getInstance());
    } else {
      if (left.isIntegerType()) {
        final BigInteger result = integerArithmeticOperation(operator);
        return new TypedOperand(result, determineResultType(result, left));
      } else if (left.isDecimalType()) {
        final BigDecimal result = decimalArithmeticOperation(operator);
        return new TypedOperand(result, determineResultType(result, left));
      } else if (left.is(EdmDate.getInstance(), EdmDuration.getInstance(), EdmDateTimeOffset.getInstance())) {
        return dateArithmeticOperation(operator);
      } else {
        throw new ODataApplicationException("Invalid type", HttpStatusCode.BAD_REQUEST.getStatusCode(),
            Locale.ROOT);
      }
    }
  }

  private EdmType determineResultType(final Number arithmeticResult, TypedOperand leftOperand) {
    // Left and right operand have the same typed, so it is enough to check the type of the left operand
    if (leftOperand.isDecimalType()) {
      final BigDecimal value = (BigDecimal) arithmeticResult;
      if (value.compareTo(EDM_SINGLE_MIN) >= 0 && value.compareTo(EDM_SINGLE_MAX) <= 0) {
        return EdmSingle.getInstance();
      } else {
        return EdmDouble.getInstance();
      }
    } else {
      final BigInteger value = (BigInteger) arithmeticResult;

      if (value.compareTo(EDN_SBYTE_MAX) <= 0 && value.compareTo(EDM_SBYTE_MIN) >= 0) {
        return EdmSByte.getInstance();
      }
      if (value.compareTo(EDM_BYTE_MAX) <= 0 && value.compareTo(EDM_BYTE_MIN) >= 0) {
        return EdmByte.getInstance();
      }
      if (value.compareTo(EDM_INT16_MAX) <= 0 && value.compareTo(EDM_INT16_MIN) >= 0) {
        return EdmInt16.getInstance();
      }
      if (value.compareTo(EDM_INT32_MAX) <= 0 && value.compareTo(EDM_INT32_MIN) >= 0) {
        return EdmInt32.getInstance();
      }
      if (value.compareTo(EDM_INT64_MAX) <= 0 && value.compareTo(EDM_INT64_MIN) >= 0) {
        return EdmInt64.getInstance();
      }
      // Choose double instead single because precision is higher (52 bits instead of 23)
      return EdmDouble.getInstance();
    }
  }

  private VisitorOperand dateArithmeticOperation(BinaryOperatorKind operator) throws ODataApplicationException {
    VisitorOperand result = null;

    if (left.is(EdmDate.getInstance())) {
      if (right.is(EdmDate.getInstance()) && operator == BinaryOperatorKind.SUB) {
        long millis = left.getTypedValue(Calendar.class).getTimeInMillis()
            - left.getTypedValue(Calendar.class).getTimeInMillis();

        result = new TypedOperand(new BigDecimal(millis).divide(FACTOR_SECOND), EdmDuration.getInstance());
      } else if (right.is(EdmDuration.getInstance()) && operator == BinaryOperatorKind.ADD) {
        long millis = left.getTypedValue(Calendar.class).getTimeInMillis()
            + (right.getTypedValue(BigDecimal.class).longValue() * FACTOR_SECOND_INT);

        result = new TypedOperand(new Timestamp(millis), EdmDateTimeOffset.getInstance());
      } else if (right.is(EdmDuration.getInstance()) && operator == BinaryOperatorKind.SUB) {
        long millis = left.getTypedValue(Calendar.class).getTimeInMillis()
            - (right.getTypedValue(BigDecimal.class).longValue() * FACTOR_SECOND_INT);

        result = new TypedOperand(new Timestamp(millis), EdmDateTimeOffset.getInstance());
      }
    } else if (left.is(EdmDuration.getInstance())) {
      if (right.is(EdmDuration.getInstance()) && operator == BinaryOperatorKind.ADD) {
        long seconds = left.getTypedValue(BigDecimal.class).longValue()
            + right.getTypedValue(BigDecimal.class).longValue();

        result = new TypedOperand(new BigDecimal(seconds), EdmDuration.getInstance());
      } else if (right.is(EdmDuration.getInstance()) && operator == BinaryOperatorKind.SUB) {
        long seconds = left.getTypedValue(BigDecimal.class).longValue()
            - right.getTypedValue(BigDecimal.class).longValue();

        result = new TypedOperand(new BigDecimal(seconds), EdmDuration.getInstance());
      }
    } else if (left.is(EdmDateTimeOffset.getInstance())) {
      if (right.is(EdmDuration.getInstance()) && operator == BinaryOperatorKind.ADD) {
        long millis = left.getTypedValue(Timestamp.class).getTime()
            + (right.getTypedValue(BigDecimal.class).longValue() * FACTOR_SECOND_INT);

        result = new TypedOperand(new Timestamp(millis), EdmDateTimeOffset.getInstance());
      } else if (right.is(EdmDuration.getInstance()) && operator == BinaryOperatorKind.SUB) {
        long millis = left.getTypedValue(Timestamp.class).getTime()
            - (right.getTypedValue(BigDecimal.class).longValue() * FACTOR_SECOND_INT);

        result = new TypedOperand(new Timestamp(millis), EdmDateTimeOffset.getInstance());
      } else if (right.is(EdmDateTimeOffset.getInstance()) && operator == BinaryOperatorKind.SUB) {
        long millis = left.getTypedValue(Timestamp.class).getTime()
            - right.getTypedValue(Timestamp.class).getTime();

        result = new TypedOperand(new BigDecimal(millis).divide(FACTOR_SECOND), EdmDuration.getInstance());
      }
    }

    if (result == null) {
      throw new ODataApplicationException("Invalid operation / operand", HttpStatusCode.BAD_REQUEST.getStatusCode(),
          Locale.ROOT);
    } else {
      return result;
    }
  }

  private BigDecimal decimalArithmeticOperation(BinaryOperatorKind operator) throws ODataApplicationException {
    final BigDecimal left = this.left.getTypedValue(BigDecimal.class);
    final BigDecimal right = this.right.getTypedValue(BigDecimal.class);

    switch (operator) {
    case ADD:
      return left.add(right);
    case DIV:
      return left.divide(left);
    case MUL:
      return left.multiply(right);
    case SUB:
      return left.subtract(right);
    default:
      throw new ODataApplicationException("Operator not valid", HttpStatusCode.BAD_REQUEST.getStatusCode(),
          Locale.ROOT);
    }
  }

  private BigInteger integerArithmeticOperation(BinaryOperatorKind operator) throws ODataApplicationException {
    final BigInteger left = this.left.getTypedValue(BigInteger.class);
    final BigInteger right = this.right.getTypedValue(BigInteger.class);

    switch (operator) {
    case ADD:
      return left.add(right);
    case DIV:
      return left.divide(right);
    case MUL:
      return left.multiply(right);
    case SUB:
      return left.subtract(right);
    case MOD:
      return left.mod(right);
    default:
      throw new ODataApplicationException("Operator not valid", HttpStatusCode.BAD_REQUEST.getStatusCode(),
          Locale.ROOT);
    }
  }
}