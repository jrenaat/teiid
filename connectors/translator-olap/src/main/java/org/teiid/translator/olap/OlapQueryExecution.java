/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.translator.olap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.olap4j.Axis;
import org.olap4j.Cell;
import org.olap4j.CellSet;
import org.olap4j.CellSetAxis;
import org.olap4j.OlapConnection;
import org.olap4j.OlapStatement;
import org.olap4j.Position;
import org.olap4j.metadata.Member;
import org.teiid.language.Argument;
import org.teiid.language.Command;
import org.teiid.language.visitor.SQLStringVisitor;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ProcedureExecution;
import org.teiid.translator.TranslatorException;

/**
 * Executes the given MDX and packs the results into an array
 */
public class OlapQueryExecution implements ProcedureExecution {

	protected Command command;
    protected OlapConnection connection;
    protected ExecutionContext context;
    protected OlapExecutionFactory executionFactory;
    private OlapStatement stmt;
    private CellSet cellSet;
    private CellSetAxis columnsAxis;
    private int colWidth;
    private ListIterator<Position> rowPositionIterator;
    private String mdxQuery;
    private boolean returnsArray;
    
	public OlapQueryExecution(List<Argument> arguments, Command command, OlapConnection connection, ExecutionContext context, OlapExecutionFactory executionFactory, String mdxQuery, boolean returnsArray) {
		this.mdxQuery = mdxQuery;
		if (arguments.size() > 0 || !returnsArray) { //TODO this is a hack at backwards compatibility 
			StringBuilder buffer = new StringBuilder();
			List<Object> parts = SQLStringVisitor.parseNativeQueryParts(this.mdxQuery, arguments);
			for (Object o : parts) {
				if (o instanceof String) {
					buffer.append(o);
				} else {
					Integer i = (Integer)o;
					buffer.append(arguments.get(i).getArgumentValue().getValue());
				}
			}
		}
		this.command = command;
		this.connection = connection;
		this.context = context;
		this.executionFactory = executionFactory;
		this.returnsArray = returnsArray;
	}
	
	@Override
	public void execute() throws TranslatorException {
		try {
			stmt = this.connection.createStatement();
			cellSet = stmt.executeOlapQuery(mdxQuery);
			CellSetAxis rowAxis = this.cellSet.getAxes().get(Axis.ROWS.axisOrdinal());
			rowPositionIterator = rowAxis.iterator();
			columnsAxis = cellSet.getAxes().get(Axis.COLUMNS.axisOrdinal());
	    	colWidth = rowAxis.getAxisMetaData().getHierarchies().size() + this.columnsAxis.getPositions().size();
		} catch (SQLException e) {
			throw new TranslatorException(e);
		} 
	}
	
	@Override
	public void cancel() throws TranslatorException {
		try {
			OlapStatement olapStatement = this.stmt;
			if (olapStatement != null) {
				olapStatement.cancel();
			}
		} catch (SQLException e) {
			throw new TranslatorException(e);
		}		
	}

	@Override
	public void close() {
		try {
			if (this.stmt != null) {
				this.stmt.close();
				this.stmt = null;
			}
		} catch (SQLException e) {
			LogManager.logDetail(LogConstants.CTX_CONNECTOR, e, "Exception closing"); //$NON-NLS-1$
		}
	}
	
    @Override
    public List<?> next() throws TranslatorException {
    	if (!rowPositionIterator.hasNext()) {
    		return null;
    	}
    	Position rowPosition = rowPositionIterator.next();
    	Object[] result = new Object[colWidth];
    	int i = 0;
    	// add in rows axis
		List<Member> members = rowPosition.getMembers();
		for (Member member:members) {
			String columnName = member.getName();
			result[i++] = columnName;
		}

		// add col axis
		for (Position colPos : columnsAxis) {
			Cell cell = cellSet.getCell(colPos, rowPosition);
			result[i++] = cell.getValue();
		}	
		if (returnsArray) {
			ArrayList<Object[]> results = new ArrayList<Object[]>(1);
			results.add(result);
			return results;
		}
		return Arrays.asList(result);
    }  
    
    @Override
    public List<?> getOutputParameterValues() throws TranslatorException {
        return null;
    }
    
}
