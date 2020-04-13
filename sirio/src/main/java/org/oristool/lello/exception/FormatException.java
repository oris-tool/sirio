/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2020 The ORIS Authors.
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
 * Describes an exceptional event such as the attempt to choose an illegal name
 * for an identifier.
 */
public class FormatException extends RuntimeException {

    private static final long serialVersionUID = 5412628585915767899L;

    public FormatException() {
        super();
    }

    public FormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatException(String message) {
        super(message);
    }

    public FormatException(Throwable cause) {
        super(cause);
    }
}
