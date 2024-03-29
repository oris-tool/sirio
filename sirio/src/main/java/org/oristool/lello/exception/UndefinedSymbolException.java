/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2021 The ORIS Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.oristool.lello.exception;

/**
 * Describes an exceptional event occurred as a result of referencing an
 * undefined symbol.
 */
public class UndefinedSymbolException extends RuntimeException {

    private static final long serialVersionUID = 5035794596586228802L;

    public UndefinedSymbolException() {
        super();
    }

    public UndefinedSymbolException(String message, Throwable cause) {
        super(message, cause);
    }

    public UndefinedSymbolException(String message) {
        super(message);
    }

    public UndefinedSymbolException(Throwable cause) {
        super(cause);
    }
}
