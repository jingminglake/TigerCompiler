package ast.optimizations;

import java.util.LinkedList;

import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec.DecSingle;
import ast.Ast.Exp;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;
import ast.Ast.Exp.*;
import ast.Ast.Stm.*;
import ast.Ast.Type.*;

// Dead code elimination optimizations on an AST.

public class DeadCode implements ast.Visitor {
	public ast.Ast.Class.T newClass;
	public ast.Ast.MainClass.T mainClass;
	public ast.Ast.Stm.T newStm;
	public ast.Ast.Method.T newMethod;
	public ast.Ast.Program.T program;
	public boolean isChange;

	public DeadCode() {
		this.newClass = null;
		this.mainClass = null;
		this.newStm = null;
		this.newMethod = null;
		this.program = null;
		this.isChange = false;
	}

	// //////////////////////////////////////////////////////
	//
	public String genId() {
		return util.Temp.next();
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(Add e) {
	}

	@Override
	public void visit(And e) {
	}

	@Override
	public void visit(ArraySelect e) {
	}

	@Override
	public void visit(Call e) {
		return;
	}

	@Override
	public void visit(False e) {
	}

	@Override
	public void visit(Id e) {
		return;
	}

	@Override
	public void visit(Length e) {
	}

	@Override
	public void visit(Lt e) {
		return;
	}

	@Override
	public void visit(NewIntArray e) {
	}

	@Override
	public void visit(NewObject e) {
		return;
	}

	@Override
	public void visit(Not e) {
	}

	@Override
	public void visit(Num e) {
		return;
	}

	@Override
	public void visit(Sub e) {
		return;
	}

	@Override
	public void visit(This e) {
		return;
	}

	@Override
	public void visit(Times e) {

		return;
	}

	@Override
	public void visit(True e) {
	}

	// statements
	@Override
	public void visit(Assign s) {
        this.newStm = s;
		return;
	}

	@Override
	public void visit(AssignArray s) {
		this.newStm = s;
		return;
	}

	@Override
	public void visit(Block s) {
		  LinkedList<ast.Ast.Stm.T> newStms = new LinkedList<ast.Ast.Stm.T>();   
		  for(ast.Ast.Stm.T stm : s.stms)
		  {
			  stm.accept(this);
			  if(this.newStm != null)
				  newStms.add(this.newStm);
			  else this.isChange = true;
		  }
		  this.newStm = new ast.Ast.Stm.Block(newStms);
		  return;
	}

	@Override
	public void visit(If s) {
        ast.Ast.Exp.T condition = s.condition;
        if (condition instanceof Exp.True) {
        	s.thenn.accept(this);
        	return;
        }
        if (condition instanceof Exp.False) {
        	s.elsee.accept(this);
        	return;
        }
        s.thenn.accept(this);
        ast.Ast.Stm.T thenn = this.newStm;
        s.elsee.accept(this);
        ast.Ast.Stm.T elsee = this.newStm;
        this.newStm = new ast.Ast.Stm.If(condition, thenn, elsee);
		return;
	}

	@Override
	public void visit(Print s) {
		this.newStm = s;
		return;
	}

	@Override
	public void visit(While s) {
		ast.Ast.Exp.T condition = s.condition;
		if (condition instanceof Exp.False) {
			this.newStm = null;
		}
		else {
			s.body.accept(this);
			this.newStm = new ast.Ast.Stm.While(condition, this.newStm);
		}
		return;
	}

	// type
	@Override
	public void visit(Boolean t) {
	}

	@Override
	public void visit(ClassType t) {
	}

	@Override
	public void visit(Int t) {
	}

	@Override
	public void visit(IntArray t) {
	}

	// dec
	@Override
	public void visit(DecSingle d) {
		return;
	}

	// method
	@Override
	public void visit(MethodSingle m) {
		LinkedList<ast.Ast.Stm.T> newStms = new LinkedList<ast.Ast.Stm.T>();
		for (ast.Ast.Stm.T stm : m.stms) {
			stm.accept(this);
			if (this.newStm != null) {
				newStms.add(this.newStm);
			}
			else this.isChange = true;
		}
		this.newMethod = new ast.Ast.Method.MethodSingle(m.retType, m.id, m.formals, m.locals, newStms, m.retExp);
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c) {
        LinkedList<ast.Ast.Method.T> newMethods = new LinkedList<ast.Ast.Method.T>();
        for (ast.Ast.Method.T method : c.methods) {
        	method.accept(this);
        	newMethods.add(this.newMethod);
        }
        this.newClass = new ast.Ast.Class.ClassSingle(c.id, c.extendss, c.decs, newMethods);
		return;
	}

	// main class
	@Override
	public void visit(MainClassSingle c) {
        c.stm.accept(this);
        this.mainClass = new ast.Ast.MainClass.MainClassSingle(c.id, c.arg, this.newStm);
		return;
	}

	// program
	@Override
	public void visit(ProgramSingle p) {

		// You should comment out this line of code:
		this.program = p;
		p.mainClass.accept(this);
        ast.Ast.MainClass.T mc = this.mainClass;
        LinkedList<ast.Ast.Class.T> newClasses = new LinkedList<ast.Ast.Class.T>();
        for (ast.Ast.Class.T clazz : p.classes) {
        	clazz.accept(this);
        	newClasses.add(this.newClass);
        }
        this.program = new ast.Ast.Program.ProgramSingle(mc, newClasses);
		if (control.Control.trace.contains("ast.DeadCode")) {
			System.out.println("before optimization:");
			ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
			p.accept(pp);
			System.out.println("after optimization:");
			this.program.accept(pp);
		}
		return;
	}
}
