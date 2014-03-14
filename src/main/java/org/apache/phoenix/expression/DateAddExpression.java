/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.expression;

import java.math.BigDecimal;
import java.util.List;

import org.apache.hadoop.hbase.io.ImmutableBytesWritable;

import org.apache.phoenix.query.QueryConstants;
import org.apache.phoenix.schema.ColumnModifier;
import org.apache.phoenix.schema.PDataType;
import org.apache.phoenix.schema.tuple.Tuple;


public class DateAddExpression extends AddExpression {
    static private final BigDecimal BD_MILLIS_IN_DAY = BigDecimal.valueOf(QueryConstants.MILLIS_IN_DAY);
    
    public DateAddExpression() {
    }

    public DateAddExpression(List<Expression> children) {
        super(children);
    }

    @Override
    public boolean evaluate(Tuple tuple, ImmutableBytesWritable ptr) {
        long finalResult=0;
        
        for(int i=0;i<children.size();i++) {
            if (!children.get(i).evaluate(tuple, ptr)) {
                return false;
            }
            if (ptr.getLength() == 0) {
                return true;
            }
            long value;
            PDataType type = children.get(i).getDataType();
            ColumnModifier columnModifier = children.get(i).getColumnModifier();
            if (type == PDataType.DECIMAL) {
                BigDecimal bd = (BigDecimal)PDataType.DECIMAL.toObject(ptr, columnModifier);
                value = bd.multiply(BD_MILLIS_IN_DAY).longValue();
            } else if (type.isCoercibleTo(PDataType.LONG)) {
                value = type.getCodec().decodeLong(ptr, columnModifier) * QueryConstants.MILLIS_IN_DAY;
            } else if (type.isCoercibleTo(PDataType.DOUBLE)) {
                value = (long)(type.getCodec().decodeDouble(ptr, columnModifier) * QueryConstants.MILLIS_IN_DAY);
            } else {
                value = type.getCodec().decodeLong(ptr, columnModifier);
            }
            finalResult += value;
        }
        byte[] resultPtr = new byte[getDataType().getByteSize()];
        ptr.set(resultPtr);
        getDataType().getCodec().encodeLong(finalResult, ptr);
        return true;
    }

    @Override
    public final PDataType getDataType() {
        return PDataType.DATE;
    }

}