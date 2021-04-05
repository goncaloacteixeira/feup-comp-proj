/*
  Copyright 2021 SPeCS.

  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
  the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
  specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.comp.jmm.ast;

import java.util.function.BiFunction;

import pt.up.fe.comp.jmm.JmmNode;

/**
 * This interface attempts to implement a visitor methodology to the JmmNodes.
 * Each type of Node will have a method associated with it.
 * When a visit occurs, depending on the type of Node, a different method will be executed
 * 
 * @author JBispo
 *
 * @param <D> Data type: String
 * @param <R> Return type: String
 */
public interface JmmVisitor<D, R> {

    /**
     * Visits a JmmNode using the visitor methodology
     * @param jmmNode Node to be visited
     * @param data Data to use during the visit. e.g. Symbol Table
     * @return Any kind of data
     */
    R visit(JmmNode jmmNode, D data);

    /**
     * Default visit in case there is no data
     * @param jmmNode Node to be visited
     * @return Any kind of data
     */
    default R visit(JmmNode jmmNode) {
        return visit(jmmNode, null);
    }

    /**
     * Depending on the Kind of Node, adds a different method of visiting
     * @param kind String representing the kind of the Node
     * @param method method to be used when visiting this kind of node
     */
    void addVisit(String kind, BiFunction<JmmNode, D, R> method);

    /**
     * Sets a method to be used when visiting a Node that does not have a method specified to its kind
     * @param method method to be used
     */
    void setDefaultVisit(BiFunction<JmmNode, D, R> method);
}
