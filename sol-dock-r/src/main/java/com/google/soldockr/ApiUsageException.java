/*
 * Copyright (C) 2012 sol-dock-r authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.soldockr;

public class ApiUsageException extends SolDockRException {

  private static final long serialVersionUID = 3697733372257568538L;

  public ApiUsageException() {
    super();
  }

  public ApiUsageException(String message, Throwable cause) {
    super(message, cause);
  }

  public ApiUsageException(String message) {
    super(message);
  }

  public ApiUsageException(Throwable cause) {
    super(cause);
  }
  
}
