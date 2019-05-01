/*
 * Copyright (c) 2019, guanquan.wang@yandex.com All Rights Reserved.
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

package cn.ttzero.excel.entity;

import cn.ttzero.excel.reader.Cell;
import cn.ttzero.excel.util.StringUtil;
import cn.ttzero.excel.annotation.DisplayName;
import cn.ttzero.excel.annotation.NotExport;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by guanquan.wang at 2018-01-26 14:48
 */
public class ListSheet<T> extends Sheet {
    protected List<T> data;
    private Field[] fields;
    protected int start, end;
    protected boolean eof;

    /**
     * Constructor worksheet
     */
    public ListSheet() {
        super();
    }

    /**
     * Constructor worksheet
     * @param name the worksheet name
     */
    public ListSheet(String name) {
        super(name);
    }

    /**
     * Constructor worksheet
     * @param name the worksheet name
     * @param columns the header info
     */
    public ListSheet(String name, final Column[] columns) {
        super(name, columns);
    }

    /**
     * Constructor worksheet
     * @param name the worksheet name
     * @param waterMark the water mark
     * @param columns the header info
     */
    public ListSheet(String name, WaterMark waterMark, final Column[] columns) {
        super(name, waterMark, columns);
    }

    public ListSheet<T> setData(final List<T> data) {
        this.data = data;
        if (!headerReady && workbook != null) {
            getHeaderColumns();
        }
        // Has data and worksheet can write
        // Paging in advance
        if (data != null && sheetWriter != null) {
            paging();
        }
        return this;
    }

    /**
     * Returns the first not null object
     * @return the object
     */
    protected T getFirst() {
        if (data == null) return null;
        T first = data.get(start);
        if (first != null) return first;
        int i = start + 1;
        do {
            first = data.get(i++);
        } while (first == null);
        return first;
    }

    /**
     * Release resources
     * @throws IOException if io error occur
     */
    @Override
    public void close() throws IOException {
        if (shouldClose) {
            data.clear();
            data = null;
        }
        super.close();
    }

    /**
     * Reset the row-block data
     */
    @Override
    protected void resetBlockData() {
        if (!eof && size() < getRowBlockSize()) {
            append();
        }

        // Find the end index of row-block
        int end = getEndIndex();
        int len = columns.length;
        try {
            for (; start < end; rows++, start++) {
                Row row = rowBlock.next();
                row.index = rows;
                Field field;
                Cell[] cells = row.realloc(len);
                for (int i = 0; i < len; i++) {
                    field = fields[i];
                    // clear cells
                    Cell cell = cells[i];
                    cell.clear();

                    Object e = field.get(data.get(start));
                    // blank cell
                    if (e == null) {
                        cell.setBlank();
                        continue;
                    }

                    setCellValueAndStyle(cell, e, columns[i]);
                }
            }
        } catch (IllegalAccessException e) {
            throw new ExcelWriteException(e);
        }
    }

    protected void append() {
        int rbs = getRowBlockSize(), size = size();
        for ( ; ; ) {
            List<T> list = more();
            if (list == null || list.isEmpty()) {
                eof = shouldClose = true;
                break;
            }
            if (data == null) {
                data = new ArrayList<>(rbs);
                setData(list);

                if (list.size() < rbs) continue;
            }
            if (start > 0 && size > 0) {
                // append and resize
                List<T> last = new ArrayList<>(size);
                last.addAll(data.subList(start, end));
                data.clear();
                data.addAll(last);
            } else if (start > 0) data.clear();
            data.addAll(list);
            start = 0;
            end = data.size();
            // Split worksheet
            if (end >= rbs) {
                paging();
                break;
            }
        }
    }

    private static final String[] exclude = {"serialVersionUID", "this$0"};

    private Field[] init() {
        T o = getFirst();
        if (o == null) return null;
        if (columns == null || columns.length == 0) {
            Field[] fields = o.getClass().getDeclaredFields();
            List<Column> list = new ArrayList<>(fields.length);
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                String gs = field.toGenericString();
                NotExport notExport = field.getAnnotation(NotExport.class);
                if (notExport != null || StringUtil.indexOf(exclude, gs.substring(gs.lastIndexOf('.') + 1)) >= 0) {
                    fields[i] = null;
                    continue;
                }
                DisplayName dn = field.getAnnotation(DisplayName.class);
                if (dn != null && StringUtil.isNotEmpty(dn.value())) {
                    list.add(new Column(dn.value(), field.getName(), field.getType()).setShare(dn.share()));
                } else {
                    list.add(new Column(field.getName(), field.getName(), field.getType()).setShare(dn != null && dn.share()));
                }
            }
            columns = new Column[list.size()];
            list.toArray(columns);
            for (int i = 0; i < columns.length; i++) {
                columns[i].styles = workbook.getStyles();
            }
            // clear not export fields
            for (int len = fields.length, n = len - 1; n >= 0; n--) {
                if (fields[n] != null) {
                    fields[n].setAccessible(true);
                    continue;
                }
                if (n < len - 1) {
                    System.arraycopy(fields, n + 1, fields, n, len - n - 1);
                }
                len--;
            }
            return fields;
        } else {
            Field[] fields = new Field[columns.length];
            Class<?> clazz = o.getClass();
            for (int i = 0; i < columns.length; i++) {
                Column hc = columns[i];
                try {
                    fields[i] = clazz.getDeclaredField(hc.key);
                    fields[i].setAccessible(true);
                    if (hc.getClazz() == null) {
                        hc.setClazz(fields[i].getType());
                    }
                } catch (NoSuchFieldException e) {
                    throw new ExcelWriteException("Column " + hc.getName() + " not declare in class " + clazz);
                }
            }
            return fields;
        }

    }

    /**
     * Returns the header column info
     * @return array of column
     */
    @Override
    public Column[] getHeaderColumns() {
        if (!headerReady) {
            if (data == null || data.isEmpty()) {
                columns = new Column[0];
            }
            // create header columns
            fields = init();
            if (fields == null || fields.length == 0 || fields[0] == null) {
                columns = new Column[0];
            } else {
                headerReady = true;
            }
        }
        return columns;
    }

    /**
     * Returns the end index of row-block
     * @return the end index
     */
    protected int getEndIndex() {
        int blockSize = getRowBlockSize(), rowLimit = sheetWriter.getRowLimit();
        if (rows + blockSize > rowLimit) {
            blockSize = rowLimit - rows;
        }
        int end = start + blockSize;
        return end <= this.end ? end : this.end;
    }

    /**
     * Returns total rows in this worksheet
     * @return -1 if unknown
     */
    @Override
    public int size() {
        return end - start;
    }

    /**
     * Split worksheet data
     */
    protected void paging() {
        int len = dataSize(), limit = sheetWriter.getRowLimit() - 1;
        // paging
        if (len > limit) {
            // Reset current index
            end = limit - rows + start;
            shouldClose = false;
            eof = true;

            int n = id;
            for (int i = end; i < len; ) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                ListSheet<T> copy = getClass().cast(clone());
                copy.start = i;
                copy.end = (i = i + limit < len ? i + limit : len);
                copy.eof = copy.size() == limit;
                workbook.insertSheet(n++, copy);
            }
            // Close on the last copy worksheet
            workbook.getSheetAt(n - 1).shouldClose = true;
        } else end = len;
    }

    /**
     * Returns total data size before split
     * @return the total size
     */
    public int dataSize() {
        return data != null ? data.size() : 0;
    }

    /**
     * Get more row data if hasMore returns true
     * @return the row data
     */
    protected List<T> more() {
        return null;
    }
}