/* This program is part of the ORIS Tool.
 * Copyright (C) 2011-2018 The ORIS Authors.
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

package org.oristool.analyzer;

import java.util.LinkedList;
import java.util.List;

/**
 * A collector of error messages for the user. 
 */
public final class ValidationMessageCollector {

    public enum Level {
        ERROR, WARNING
    }

    public class Message {
        private Level level;
        private String text;

        public Message(Level level, String text) {
            this.level = level;
            this.text = text;
        }

        public Level getLevel() {
            return level;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return level + ": " + text;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }
            
            if (!(obj instanceof Message)) {
                return false;
            }
            
            Message other = (Message) obj;

            if (this.level != other.level || !this.text.equals(other.text)) {
                return false;
            }
            
            return true;
        }

        @Override
        public int hashCode() {

            int result = 17;
            result = 31 * result + this.level.hashCode();
            result = 31 * result + this.text.hashCode();
            return result;
        }
    }

    private List<Message> messages = new LinkedList<Message>();

    public ValidationMessageCollector() {
    }

    public void addMessage(Level level, String text) {
        messages.add(new Message(level, text));
    }

    public void addWarning(String text) {
        this.addMessage(Level.WARNING, text);
    }

    public void addError(String text) {
        this.addMessage(Level.ERROR, text);
    }

    public List<Message> getMessages() {
        return messages;
    }
}
