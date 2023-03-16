/*
 * Copyright (c) 2017-2020, guanquan.wang@yandex.com All Rights Reserved.
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


package org.ttzero.excel.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author guanquan.wang at 2020-05-21 16:43
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HeaderComment {
    /**
     * Description detail
     *
     * @return the comment detail desc
     */
    String value() default "";

    /**
     * Specify a title, the title will be bold display
     *
     * @return sub-title of comment
     */
    String title() default "";

    /**
     * Specify the comment width
     *
     * @return comment width
     */
    double width() default 100.8D;

    /**
     * Specify the comment height
     *
     * @return comment height
     */
    double height() default 60.6D;
}
