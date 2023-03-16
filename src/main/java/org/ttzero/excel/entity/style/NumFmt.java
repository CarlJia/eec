/*
 * Copyright (c) 2017-2018, guanquan.wang@yandex.com All Rights Reserved.
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

package org.ttzero.excel.entity.style;

import org.dom4j.Element;
import org.ttzero.excel.util.StringUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.ttzero.excel.entity.style.Styles.getAttr;

/**
 * To create a custom number format, you start by selecting one of the built-in number formats as a starting point.
 * You can then change any one of the code sections of that format to create your own custom number format.
 * <p>
 * A number format can have up to four sections of code, separated by semicolons.
 * These code sections define the format for positive numbers, negative numbers, zero values, and text, in that order.
 * <p>
 * &lt;POSITIVE&gt;;&lt;NEGATIVE&gt;;&lt;ZERO&gt;;&lt;TEXT&gt;
 * <p>
 * For example, you can use these code sections to create the following custom format:
 * <p>
 * [Blue]#,##0.00_);[Red](#,##0.00);0.00;"sales "@
 * <p>
 * You do not have to include all code sections in your custom number format.
 * If you specify only two code sections for your custom number format,
 * the first section is used for positive numbers and zeros, and the second section is used for negative numbers.
 * If you specify only one code section, it is used for all numbers.
 * If you want to skip a code section and include a code section that follows it,
 * you must include the ending semicolon for the section that you skip.
 * <ul>
 * <li><a href="https://support.office.com/en-us/article/create-or-delete-a-custom-number-format-78f2a361-936b-4c03-8772-09fab54be7f4">Create a custom number format</a></li>
 * <li><a href="https://support.office.com/en-us/article/Number-format-codes-5026bbd6-04bc-48cd-bf33-80f18b4eae68?ui=en-US&rs=en-US&ad=US">Number format codes</a></li>
 * <li><a href="https://docs.microsoft.com/en-us/previous-versions/office/developer/office-2010/ee857658(v=office.14)">NumberingFormat Class</a></li>
 * </ul>
 *
 * @author guanquan.wang at 2018-02-06 08:51
 */
public class NumFmt implements Comparable<NumFmt> {

    /**
     * Format as {@code yyyy-mm-dd hh:mm:ss}
     */
    public static final NumFmt DATETIME_FORMAT = new NumFmt("yyyy\\-mm\\-dd\\ hh:mm:ss"),
    /**
     * Format as {@code yyyy-mm-dd}
     */
    DATE_FORMAT = new NumFmt("yyyy\\-mm\\-dd"),
    /**
     * Format as {@code hh:mm:ss}
     */
    TIME_FORMAT = new NumFmt("hh:mm:ss");

    protected String code;
    protected int id = -1;

    public NumFmt() { }

    NumFmt(int id, String code) {
        this.id = id;
        this.code = code;
    }

    public NumFmt(String code) {
        this.code = clean(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    NumFmt setId(int id) {
        this.id = id;
        return this;
    }

    public int getId() {
        return id;
    }

    /**
     * Built-In number format
     *
     * @param id the built-in id
     * @return the {@link NumFmt}
     */
    public static NumFmt valueOf(int id) {
        return new NumFmt().setId(id);
    }

    /**
     * Create a NumFmt
     *
     * @param code the numFmt code string
     * @return NumFmt
     */
    public static NumFmt of(String code) {
        return new NumFmt(code);
    }

    // Clean the format code
    private static String clean(String code) {
        if (StringUtil.isEmpty(code))
            throw new NumberFormatException("The format code must not be null or empty.");

        // Replace '-' to '\-'
        code = escape(code, '-');
        // Replace ' ' to '\ '
        code = escape(code, ' ');

        return code;
    }

    private static String escape(String code, char c) {
        int i = code.indexOf(c);
        if (i > -1) {
            int j = 0;
            StringBuilder buf = new StringBuilder();
            do {
                if (i != j) {
                    buf.append(code, j, i);
                    j = i;
                }
                if (i == 0 || code.charAt(i - 1) != '\\') {
                    buf.append('\\');
                }
            } while ((i = code.indexOf(c, i + 1)) > -1);
            code = buf.append(code, j, code.length()).toString();
        }
        return code;
    }

    /**
     * Roughly calculate the cell length
     *
     * @param base the cell value length
     * @return cell length
     */
    public double calcNumWidth(double base) {
        if (StringUtil.isBlank(code)) return 0.0D;
        // Calculate the segment length separately and return the maximum value
        String[] codes = code.split(";");
        double max = 0.0D;
        for (String code : codes) {
            double n = base < 0.0D ? 1.0D : 0.0D;
            boolean ignore = false, comma = false;
            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                if (c == '"' || c == '\\') continue;
                if (ignore) {
                    if (c == ']' || c == ')') {
                        ignore = false;
                    }
                    continue;
                }
                if (c == '[' || c == '(') {
                    ignore = true;
                    continue;
                }
                if (c == ',') comma = true;
                n += c > 0x4E00 ? 1.86D : 1.0D;
            }
            // Test date format
            boolean isDate = Styles.testCodeIsDate(code);
            int k = 0;
            if (!isDate) {
                k = code.lastIndexOf('.');
                if (k < 0) {
                    k = code.length();
                    for (; k > 0; k--) {
                        char c = code.charAt(k - 1);
                        if (!(c == '_' || c == ' ' || c == '.')) break;
                    }
                }
                k = k >= 0 ? code.length() - k : 0;
            }
            double len = isDate ? n : (comma ? base + base / 3 : base) + k;
            if (max < len) max = len;
        }
        return max + 0.86D;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NumFmt) {
            NumFmt other = (NumFmt) o;
            return Objects.equals(other.code, code);
        }
        return false;
    }

    @Override
    public String toString() {
        return "id: " + id + ", code: " + code;
    }

    public Element toDom4j(Element root) {
        if (StringUtil.isEmpty(code)) return root; // Build in style
        return root.addElement(StringUtil.lowFirstKey(getClass().getSimpleName()))
            .addAttribute("formatCode", code)
            .addAttribute("numFmtId", String.valueOf(id));
    }

    public static List<NumFmt> domToNumFmt(Element root) {
        // Number format
        Element ele = root.element("numFmts");
        // Break if there don't contains 'numFmts' tag
        if (ele == null) {
            return new ArrayList<>();
        }
        List<Element> sub = ele.elements();
        List<NumFmt> numFmts = new ArrayList<>(sub.size());
        for (Element e : sub) {
            String id = getAttr(e, "numFmtId"), code = getAttr(e, "formatCode");
            numFmts.add(new NumFmt(Integer.parseInt(id), code));
        }
        // Sort by id
        numFmts.sort(Comparator.comparingInt(NumFmt::getId));
        return numFmts;
    }

    @Override
    public int compareTo(NumFmt o) {
        return id - o.id;
    }
}
