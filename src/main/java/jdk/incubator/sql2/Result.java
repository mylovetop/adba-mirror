/*
 * Copyright (c)  2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.incubator.sql2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 *
 */
public interface Result {

  /**
   * A {@link Result} that is just a number of rows modified, a {@link Long}.
   *
   * Note: It is certainly true that this is not needed; {@link Long} could be
   * used instead. Seems like there might be a documentational advantage to
   * having this type. If you don't like it, just mentally replace it with
   * {@link Long} everywhere it appears.
   */
  public static interface RowCount extends Result {

    /**
     *
     * @return
     */
    public long getCount();
  }

  /**
   * A mutable handle to one value of an ordered sequence of columns of a row or
   * of out parameters. Columns have a 1-based index and optionally an
   * identifier. Identifiers are not guaranteed to be unique. Only {@code clone}
   * and {@code slice} create new instances. All other methods return this
   * instance (modifying it if necessary) including {@code forEach},
   * {@code next}, and {@code iterator}.
   */
  public interface Column extends Result, Iterable<Column>, Iterator<Column>, Cloneable {

    /**
     * Return the value of this column as an instance of the given type.
     *
     * @param <T>
     * @param type
     * @return the value of this {@link Column}
     */
    public <T> T get(Class<T> type);

    /**
     * Return the value of this {@link Column} as an instance of the default
     * Java type for this column.
     *
     * @param <T>
     * @return the value of this {@link Column}
     */
    public default <T> T get() {
      return get(javaType());
    }

    /**
     * Return the identifier of this {@link Column}. May be null.
     *
     * @return the identifier of this {@link Column}. May be null
     */
    public String identifier();

    /**
     * Return the 1-based index of this {@link Column}. The returned value is
     * relative to the slice if this {@link Column} is the result of a call to
     * {@code slice()}. {@code
     * col.slice(n).index() == 1}.
     *
     * @return the index of this {@link Column}
     */
    public int index();

    /**
     * Return the 1-based index of this {@link Column} relative to the original
     * sequence of values.
     * {@code col.absoluteIndex() == col.slice(n).absoluteIndex()}.
     *
     * @return the absolute 1-based index of this {@link Column}
     */
    public int absoluteIndex();

    /**
     * Return the SQL type of the value of this {@link Column}.
     *
     * @return the SQL type of this value
     */
    public SqlType sqlType();

    /**
     * Return the Java type that best represents the value of this
     * {@link Column}.
     *
     * @param <T>
     * @return a {@link Class} that best represents the value of this
     * {@link Column}
     */
    public <T>Class<T> javaType();

    /**
     * The length of the current value if defined.
     *
     * @return
     * @throws UnsupportedOperationException if the length of the current value
     * is undefined
     */
    public long length();

    /**
     * Return the number of remaining values accessible by this {@link Column}
     * excluding the current value. This is the number of times {@code next()}
     * can be called before {@code hasNext()} returns false.
     *
     * @return the number of values remaining
     */
    public int numberOfValuesRemaining();

    /**
     * Modify this {@link Column} to point to a value identified by id.
     *
     * @apiNote The value specified for {@code id} represents the name of a
     * column or parameter marker for the underlying data source and is
     * implementation specific. This may be a simple SQL identifier, a quoted
     * identifier, or any other type of identifier supported by the data source.
     * <p>
     * Consult your implementation&#39;s documentation for additional
     * information.
     *
     * @param id an identifier. Not null
     * @return this {@link Column}
     * @throws NoSuchElementException if id does not identify exactly one value
     */
    public Column at(String id);

    /**
     * Modify this {@link Column} to point to the value at {@code index}. The
     * first value is at index 1. Negative numbers count back from the last
     * value. The last value is at index -1.
     *
     * @param index a new index
     * @return this {@link Column}
     * @throws NoSuchElementException if {@code index > length} or
     * {@code index < -length}
     */
    public Column at(int index);

    /**
     * Modify this {@link Column} to point to the value at the current index +
     * {@code offset}. If {@code offset} is 0 this is a noop. If {@code offset}
     * is negative the new index is less than the current index. If the new
     * index would be less than 1 or greater than length this {@link Column} is
     * not modified and {@link IllegalArgumentException} is thrown.
     *
     * @param offset an increment to the current index
     * @return this {@link Column}
     * @throws NoSuchElementException if the new index would be less than 1 or
     * greater than {@code length}
     */
    public default Column next(int offset) {
      int newIndex = index() + offset;
      if (offset > numberOfValuesRemaining() || newIndex < 1) {
        throw new NoSuchElementException();
      }
      return at(newIndex);
    }

    /**
     * Return a new {@link Column} that is a handle to a subsequence of the
     * sequence of values referenced by this {@link Column}. The subsequence
     * consists of {@code numValues} number of values. If {@code numValues} is
     * positive the values are the value of this column and its successors. If
     * {@code numValues} is negative the values are the predecessors of this
     * column not including this {@link Column}. The order of the values of the
     * new {@link Column} is the same as the order of the values of this
     * {@link Column}. The returned {@link Column} points to the first value of
     * the slice. This {@link Column} is not modified.
     *
     * @param numValues the number of columns to include in the slice
     * @return a new {@link Column}.
     * @throws NoSuchElementException if the current index plus
     * {@code numValues} is greater than the number of values of this
     * {@link Column} or less than 1
     */
    public Column slice(int numValues);

    /**
     * Return a new {@link Column} that is a duplicate of this {@link Column}.
     * This {@link Column} is not modified.
     *
     * @return a new {@link Column}
     */
    public Column clone();

    /**
     * Modify this {@link Column} to point to the next value in the sequence.
     *
     * @return this {@link Column}
     * @throws NoSuchElementException if the new index would be greater than
     * {@code length}
     */
    @Override
    public default Column next() {
      return next(1);
    }

    @Override
    public default boolean hasNext() {
      return numberOfValuesRemaining() > 0;
    }

    @Override
    public default void forEach(Consumer<? super Column> action) {
      do {
        action.accept(this);
        if (!hasNext()) break;
        next();
      } while (true);
    }

    @Override
    public default Column iterator() {
      return this.clone();
    }

    /**
     * TODO This almost certainly works correctly but it doesn't integrate well
     * with the other access patterns. A better approach would be a Spliterator
     * that overrides trySplit and creates new slices for each batch.
     *
     * There is a fundamental problem with mixing Spliterator with the other
     * access patterns. The other patterns assume navigation from one column to
     * an arbitrary other column. Spliterator.trySplit can divide the column
     * sequence in arbitrary places invalidating the assumption about column
     * navigation.
     *
     * @return a {@link Spliterator}
     */
    @Override
    public default Spliterator<Column> spliterator() {
      List list = new ArrayList<>(numberOfValuesRemaining());
      this.clone().forEach(c -> list.add(c.slice(1)));
      return java.util.Spliterators.spliterator(list.toArray(), numberOfValuesRemaining());
    }
  }

  /**
   * Used by {@link OutOperation} to expose the out parameters of a stored
   * procedure call.
   *
   * This exists to allow for future additions.
   */
  public interface OutColumn extends Column {

  }

  /**
   * Used by {@link RowOperation} to expose each row of a row sequence.
   */
  public static interface RowColumn extends Column {

    /**
     * The count of rows in the row sequence preceeding this {@link RowColumn}.
     * For the first row in the row sequence the {@code rowNumber} is 0.
     *
     * @return the count of rows in the row sequence preceeding this
     * {@link RowColumn}
     * @throws IllegalStateException if the call that was passed this
     * {@link Result} has ended
     */
    public long rowNumber();

    /**
     * Terminate processing of the rows in this {@link RowOperation}. No further
     * rows in the row sequence will be processed. All subsequent rows, if any,
     * will be ignored. Any rows already fetched will not be processed. Any rows
     * not yet fetched may or may not be fetched. If fetched they will not be
     * processed. The RowOperation will be completed normally, as though the
     * current row were the last row of the row sequence.
     *
     * @throws IllegalStateException if the call that was passed this
     * {@link RowColumn} has ended
     */
    public void cancel();

  }

}
