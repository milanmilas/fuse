/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.commands;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.fusesource.fabric.boot.commands.support.EnsembleCommandSupport;

@Command(name = "ensemble-remove", scope = "fabric", description = "Re-create the current ensemble, excluding the specified containers from the ensemble", detailedDescription = "classpath:ensemble.txt")
public class EnsembleRemove extends EnsembleCommandSupport {

    @Argument(required = true, multiValued = true, description = "List of containers to be removed. Must be an even number of containers.")
    private List<String> containers;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        service.removeFromCluster(containers);
        return null;
    }

}
