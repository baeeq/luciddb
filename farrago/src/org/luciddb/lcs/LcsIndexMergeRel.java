/*
// Licensed to DynamoBI Corporation (DynamoBI) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  DynamoBI licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at

//   http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
*/
package org.luciddb.lcs;

import net.sf.farrago.catalog.*;
import net.sf.farrago.fem.fennel.*;
import net.sf.farrago.fennel.rel.*;
import net.sf.farrago.query.*;

import org.eigenbase.rel.metadata.*;
import org.eigenbase.relopt.*;


/**
 * LcsIndexMergeRel is a relation for merging the results of an index scan. The
 * input to this relation must be a single input and it must be an
 * LcsIndexSearchRel. The input data consists of unordered rid segments. The
 * result set produced by this relation will be ordered rid segments.
 *
 * @author John Pham
 * @version $Id$
 */
class LcsIndexMergeRel
    extends FennelSingleRel
{
    //~ Instance fields --------------------------------------------------------

    final LcsTable lcsTable;
    FennelRelParamId ridLimitParamId;
    FennelRelParamId consumerSridParamId;
    FennelRelParamId segmentLimitParamId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LcsIndexMergeRel object.
     *
     * @param indexSearchRel the input to this merge
     */
    public LcsIndexMergeRel(
        LcsTable lcsTable,
        LcsIndexSearchRel indexSearchRel,
        FennelRelParamId consumerSridParamId,
        FennelRelParamId segmentLimitParamId,
        FennelRelParamId ridLimitParamId)
    {
        super(
            indexSearchRel.getCluster(),
            indexSearchRel);
        this.lcsTable = lcsTable;

        // These two parameters are used when there's an AND
        // downstream(as consumer)
        this.consumerSridParamId = consumerSridParamId;
        this.segmentLimitParamId = segmentLimitParamId;

        // This parameter is used when there's a "chopper"
        // upstream(as producer)
        this.ridLimitParamId = ridLimitParamId;
    }

    //~ Methods ----------------------------------------------------------------

    // implement Cloneable
    public LcsIndexMergeRel clone()
    {
        return new LcsIndexMergeRel(
            lcsTable,
            (LcsIndexSearchRel) getChild().clone(),
            consumerSridParamId,
            segmentLimitParamId,
            ridLimitParamId);
    }

    // implement RelNode
    public RelOptCost computeSelfCost(RelOptPlanner planner)
    {
        // TODO:  the real thing(sorter costing + merge cost)
        return planner.makeTinyCost();
    }

    // override RelNode
    public double getRows()
    {
        // the number of rows returned is the number of rows that the
        // index search input will return
        return RelMetadataQuery.getRowCount(getInput(0));
    }

    // implement FennelRel
    public FemExecutionStreamDef toStreamDef(FennelRelImplementor implementor)
    {
        FarragoRepos repos = FennelRelUtil.getRepos(this);

        //
        // First obtain the child stream
        //
        FemExecutionStreamDef search =
            implementor.visitFennelChild((FennelRel) getChild(), 0);

        //
        // Chop the tuples so they fit in memory when expanded
        //
        FemLbmChopperStreamDef chopper = repos.newFemLbmChopperStreamDef();
        chopper.setRidLimitParamId(
            implementor.translateParamId(ridLimitParamId).intValue());
        implementor.addDataFlowFromProducerToConsumer(search, chopper);

        //
        // Sort the stream
        //
        FemSortingStreamDef sorter = lcsTable.getIndexGuide().newBitmapSorter();
        implementor.addDataFlowFromProducerToConsumer(chopper, sorter);

        //
        // Merge the results
        //
        FemLbmUnionStreamDef merge = repos.newFemLbmUnionStreamDef();

        merge.setConsumerSridParamId(
            implementor.translateParamId(consumerSridParamId).intValue());
        merge.setSegmentLimitParamId(
            implementor.translateParamId(segmentLimitParamId).intValue());

        merge.setRidLimitParamId(
            implementor.translateParamId(ridLimitParamId).intValue());

        implementor.addDataFlowFromProducerToConsumer(sorter, merge);

        return merge;
    }

    //  implement RelNode
    public void explain(RelOptPlanWriter pw)
    {
        String [] names = new String[4];

        names[0] = "child#0";
        names[1] = "consumerSridParamId";
        names[2] = "segmentLimitParamId";
        names[3] = "ridLimitParamId";
        pw.explain(
            this,
            names,
            new Object[] {
                print(consumerSridParamId), print(segmentLimitParamId),
                print(ridLimitParamId)
            });
    }

    private Object print(FennelRelParamId paramId)
    {
        return (paramId == null) ? (Integer) 0 : paramId;
    }
}

// End LcsIndexMergeRel.java
