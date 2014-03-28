/*
 * Copyright (c) 2014 Faust Edition development team.
 *
 * This file is part of the Faust Edition.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.faustedition;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

import de.faustedition.json.CompactTextModule;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Component
public class ObjectMapperFactoryBean extends AbstractFactoryBean<ObjectMapper> {

  @Override
  public Class<?> getObjectType() {
    return ObjectMapper.class;
  }

  @Override
  protected ObjectMapper createInstance() throws Exception {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new CompactTextModule());
    return objectMapper;
  }
}