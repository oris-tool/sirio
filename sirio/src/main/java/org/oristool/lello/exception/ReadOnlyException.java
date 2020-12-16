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
 * Describes an exceptional event occurred during an illegal attempt to alter a
 * read-only value.
 */
public class ReadOnlyException extends RuntimeException {

    private static final long serialVersionUID = 844962332199165174L;

    public ReadOnlyException() {
        super();
    }

    public ReadOnlyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadOnlyException(String message) {
        super(message);
    }

    public ReadOnlyException(Throwable cause) {
        super(cause);
    }
}
