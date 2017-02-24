package ast.optimizations;

import java.util.LinkedList;

import ast.Ast.Class;
import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec.DecSingle;
import ast.Ast.Exp;
import ast.Ast.Exp.Add;
import ast.Ast.Exp.And;
import ast.Ast.Exp.ArraySelect;
import ast.Ast.Exp.Call;
import ast.Ast.Exp.False;
import ast.Ast.Exp.Id;
import ast.Ast.Exp.Length;
import ast.Ast.Exp.Lt;
import ast.Ast.Exp.NewIntArray;
import ast.Ast.Exp.NewObject;
import ast.Ast.Exp.Not;
import ast.Ast.Exp.Num;
import ast.Ast.Exp.Sub;
import ast.Ast.Exp.This;
import ast.Ast.Exp.Times;
import ast.Ast.Exp.True;
import ast.Ast.MainClass;
import ast.Ast.MainClass.MainClassSingle;
import ast.Ast.Method;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program;
import ast.Ast.Program.ProgramSingle;
import ast.Ast.Stm;
import ast.Ast.Stm.Assign;
import ast.Ast.Stm.AssignArray;
import ast.Ast.Stm.Block;
import ast.Ast.Stm.If;
import ast.Ast.Stm.Print;
import ast.Ast.Stm.While;
import ast.Ast.Type.Boolean;
import ast.Ast.Type.ClassType;
import ast.Ast.Type.Int;
import ast.Ast.Type.IntArray;

// Constant folding optimizations on an AST.

public class ConstFold implements ast.Visitor {
	private Class.T newClass;
	private MainClass.T mainClass;
	public ast.Ast.Method.T newMethod;
	public ast.Ast.Stm.T newStm;
	public Exp.T newExp;
	public Program.T program;
	public boolean isChange;

	public ConstFold() {
		this.newClass = null;
		this.mainClass = null;
		this.newMethod = null;
		this.newStm = null;
		this.newExp = null;
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
		Exp.T left = e.left;
		Exp.T right = e.right;
		if ((left instanceof Exp.Num) && (right instanceof Exp.Num)) {	
			this.isChange = true;
			Exp.Num leftNum = (Exp.Num) left;
			Exp.Num rightNum = (Exp.Num) right;
			this.newExp = new Exp.Num(leftNum.num + rightNum.num);
			return;
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.Add(left, right);
	}

	@Override
	public void visit(And e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if ((left instanceof Exp.False) || (right instanceof Exp.False)) {
			this.isChange = true;
			this.newExp = new Exp.False();
			return;
		}
		if ((left instanceof Exp.True) && (right instanceof Exp.True)) {
			this.isChange = true;
			this.newExp = new Exp.True();
			return;
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.And(left, right);
		return;
	}

	@Override
	public void visit(ArraySelect e) {
		e.array.accept(this);
		Exp.T array = this.newExp;
		e.index.accept(this);
		Exp.T index = this.newExp;
		this.newExp = new Exp.ArraySelect(array, index);
		return;
	}

	@Override
	public void visit(Call e) {
        this.newExp = e;
		return;
	}

	@Override
	public void visit(False e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Id e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Length e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Lt e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if ((left instanceof Exp.Num) && (right instanceof Exp.Num)) {
			this.isChange = true;
			Exp.Num leftNum = (Exp.Num)left;
			Exp.Num rightNum = (Exp.Num)right;
			if (leftNum.num < rightNum.num) {
				this.newExp = new Exp.True();
				return;
			}
			else {
				this.newExp = new Exp.False();
				return;
			}
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.Lt(left, right);
		return;
	}

	@Override
	public void visit(NewIntArray e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(NewObject e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Not e) {
		Exp.T exp = e.exp;
		if (exp instanceof Exp.True) {
			this.isChange = true;
			this.newExp = new Exp.False();
			return;
		}
		if (exp instanceof Exp.False) {
			this.isChange = true;
			this.newExp = new Exp.True();
			return;
		}
		e.exp.accept(this);
		exp = this.newExp;
		this.newExp = new Exp.Not(exp);
		return;
	}

	@Override
	public void visit(Num e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Sub e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if ((left instanceof Exp.Num) && (right instanceof Exp.Num)) {
			this.isChange = true;
			Exp.Num leftNum = (Exp.Num) left;
			Exp.Num rightNum = (Exp.Num) right;
			this.newExp = new Exp.Num(leftNum.num - rightNum.num);
			return;
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.Sub(left, right);
	}

	@Override
	public void visit(This e) {
		this.newExp = e;
		return;
	}

	@Override
	public void visit(Times e) {
		Exp.T left = e.left;
		Exp.T right = e.right;
		if ((left instanceof Exp.Num) && (right instanceof Exp.Num)) {
			this.isChange = true;
			Exp.Num leftNum = (Exp.Num) left;
			Exp.Num rightNum = (Exp.Num) right;
			this.newExp = new Exp.Num(leftNum.num * rightNum.num);
			return;
		}
		e.left.accept(this);
		left = this.newExp;
		e.right.accept(this);
		right = this.newExp;
		this.newExp = new Exp.Times(left, right);
	}

	@Override
	public void visit(True e) {
		this.newExp = e;
		return;
	}

	// statements
	@Override
	public void visit(Assign s) {
        s.exp.accept(this);
        Exp.T exp = this.newExp;
        this.newStm = new ast.Ast.Stm.Assign(s.id, exp);
		return;
	}

	@Override
	public void visit(AssignArray s) {
		s.exp.accept(this);
		Exp.T exp = this.newExp;
		s.index.accept(this);
		Exp.T index = this.newExp;
		this.newStm = new Stm.AssignArray(s.id, index, exp);
		return;
	}

	@Override
	public void visit(Block s) {
		LinkedList<Stm.T> stms = new LinkedList<Stm.T>();
		for (Stm.T stm : s.stms) {
			stm.accept(this);
			stms.add(this.newStm);
		}
		this.newStm = new Stm.Block(stms);
		return;
	}

	@Override
	public void visit(If s) {
        s.condition.accept(this);
        Exp.T condition = this.newExp;
        s.thenn.accept(this);
        Stm.T thenn = this.newStm;
        s.elsee.accept(this);
        Stm.T elsee = this.newStm;
        this.newStm = new Stm.If(condition, thenn, elsee);
		return;
	}

	@Override
	public void visit(Print s) {
		s.exp.accept(this);
		Exp.T exp = this.newExp;
		this.newStm = new Stm.Print(exp);
		return;
	}

	@Override
	public void visit(While s) {
		s.condition.accept(this);
		Exp.T conditon = this.newExp;
		s.body.accept(this);
		Stm.T body = this.newStm;
		this.newStm=new Stm.While(conditon, body);
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
		LinkedList<Stm.T> newStms = new LinkedList<Stm.T>();
		for(Stm.T stm : m.stms)
		{
			stm.accept(this);
			newStms.add(this.newStm);
		}
		m.retExp.accept(this);
		Exp.T retExp = this.newExp;
		this.newMethod = new ast.Ast.Method.MethodSingle(m.retType, m.id, m.formals, m.locals, newStms, retExp);	
	    return;
	}

	// class
	@Override
	public void visit(ClassSingle c) {
		LinkedList<ast.Ast.Method.T> newMethods = new LinkedList<ast.Ast.Method.T>();
	    for(Method.T m:c.methods)
	    {
	    	m.accept(this);
	    	newMethods.add(this.newMethod);
	    }
	    this.newClass = new ast.Ast.Class.ClassSingle(c.id, c.extendss, c.decs, newMethods);
	    return;
	}

	// main class
	@Override
	public void visit(MainClassSingle c) {
		c.stm.accept(this);
		Stm.T stm = this.newStm;
		this.mainClass = new ast.Ast.MainClass.MainClassSingle(c.id, c.arg, stm);
		return;
	}

	// program
	@Override
	public void visit(ProgramSingle p) {

		// You should comment out this line of code:
		this.program = p;
        p.mainClass.accept(this);
        ast.Ast.MainClass.T mc = this.mainClass;
        LinkedList<ast.Ast.Class.T> newClasses = new  LinkedList<ast.Ast.Class.T>();
        for (ast.Ast.Class.T clazz : p.classes) {
        	clazz.accept(this);
        	newClasses.add(this.newClass);
        }
        this.program = new ast.Ast.Program.ProgramSingle(mc, newClasses);
		if (control.Control.isTracing("ast.ConstFold")) {
			System.out.println("before optimization:");
			ast.PrettyPrintVisitor pp = new ast.PrettyPrintVisitor();
			p.accept(pp);
			System.out.println("after optimization:");
			this.program.accept(pp);
		}
		return;
	}
}
