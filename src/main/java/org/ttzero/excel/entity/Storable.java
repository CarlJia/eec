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

package org.ttzero.excel.entity;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 持久化公共接口，默认落盘处理
 *
 * @author guanquan.wang at 2019-05-08 13:13
 */
public interface Storable {
    /**
     * 写入指定路径，传入的参数可以是一个文件也可以是文件夹，需要在实现类具体处理
     *
     * @param root 父级文件夹或文件绝对路径
     * @throws IOException 写失败异常
     */
    void writeTo(Path root) throws IOException;
}
