/*
 * Copyright (c) 2017-2019, guanquan.wang@yandex.com All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ttzero.excel.reader;

import static org.ttzero.excel.entity.Sheet.int2Col;
import static org.ttzero.excel.reader.ExcelReader.cellRangeToLong;

/**
 * Worksheet dimension
 * <p>
 * This record contains the range address of the used area in the current sheet.
 *
 * @author guanquan.wang at 2019-12-20 10:07
 */
public class Dimension {
    // Index to first used row (one base)
    public final int firstRow;
    // Index to last used row (one base)
    public final int lastRow;
    // Index to first used column (one base)
    public final short firstColumn;
    // Index to last used column (one base)
    public final short lastColumn;
    // Width and Height
    public final int width, height;

    public Dimension(int firstRow, short firstColumn) {
        this.firstRow = firstRow;
        this.firstColumn = firstColumn;
        this.lastRow = 0;
        this.lastColumn = 0;

        this.width = 1;
        this.height = 1;
    }

    public Dimension(int firstRow, short firstColumn, int lastRow, short lastColumn) {
        this.firstRow = firstRow;
        this.firstColumn = firstColumn;
        this.lastRow = lastRow;
        this.lastColumn = lastColumn;

        this.width = lastColumn > 0 && firstColumn > 0 ? lastColumn - firstColumn + 1 : lastColumn > 0 ? lastColumn : 1;
        this.height = lastRow > 0 && firstRow > 0 ? lastRow - firstRow + 1 : lastRow > 0 ? lastRow : 1;
    }

    /**
     * Create {@link Dimension} from a range string
     *
     * @param range range string like {@code A2:B2}
     * @return the {@link Dimension} entry
     */
    public static Dimension of(String range) {
        int i = range.indexOf(':');

        long f = 0L, t = 0L;
        if (i < 0) {
            f = cellRangeToLong(range);
        } else if (i == 0) {
            t = cellRangeToLong(range.substring(i + 1));
        } else {
            f = cellRangeToLong(range.substring(0, i));
            t = cellRangeToLong(range.substring(i + 1));
        }
        return new Dimension((int) (f >> 16), (short) f, (int) (t >> 16), (short) t);
    }

    /**
     * Returns the index to first used row, the min value is 1
     *
     * @return the first row number
     */
    public int getFirstRow() {
        return firstRow;
    }

    /**
     * Returns the index to last used row, the max value
     * is 1,048,576 in office 2007 or later and 65,536 in office 2003
     *
     * @return the last row number
     */
    public int getLastRow() {
        return lastRow;
    }

    /**
     * Returns the index to first used column, the min value is 1
     *
     * @return the first column number
     */
    public short getFirstColumn() {
        return firstColumn;
    }

    /**
     * Returns the index to last used column, the max value
     * is 16,384 in office 2007 or later and 256 in office 2003
     *
     * @return the last column number
     */
    public short getLastColumn() {
        return lastColumn;
    }

    /**
     * Returns the width in dimension
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height in dimension
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return new String(int2Col(firstColumn)) + this.firstRow
            + (lastRow > 0 ? ":" + new String(int2Col(lastColumn)) + this.lastRow
            : "");
    }

    /**
     * Determine whether the specified coordinates are within the {@link Dimension}
     *
     * @param r row number
     * @param c column number
     * @return true if axis is in {@link Dimension}
     */
    public boolean checkRange(int r, int c) {
        return r >= firstRow && r <= lastRow && c >= firstColumn && c <= lastColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Dimension) {
            Dimension other = (Dimension) o;
            return other.firstRow == firstRow && other.firstColumn == firstColumn
                    && other.lastRow == lastRow && other.lastColumn == lastColumn;
        }
        return false;
    }
}
