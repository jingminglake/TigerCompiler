package ast;

import ast.Ast.Class.ClassSingle;
import ast.Ast.Dec;
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
import ast.Ast.Method;
import ast.Ast.Method.MethodSingle;
import ast.Ast.Program;
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

public class PrettyPrintVisitor implements Visitor {
	private int indentLevel;

	public PrettyPrintVisitor() {
		this.indentLevel = 4;
	}

	private void indent() {
		this.indentLevel += 2;
	}

	private void unIndent() {
		this.indentLevel -= 2;
	}

	private void printSpaces() {
		int i = this.indentLevel;
		while (i-- != 0)
			this.say(" ");
	}

	private void sayln(String s) {
		System.out.println(s);
	}

	private void say(String s) {
		System.out.print(s);
	}

	// /////////////////////////////////////////////////////
	// expressions
	@Override
	public void visit(Add e) {
		// Lab2, exercise4: filling in missing code.
		// Similar for other methods with empty bodies.
		// Your code here:
		e.left.accept(this);
		this.say(" + ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(And e) {
		e.left.accept(this);
		this.say(" && ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(ArraySelect e) {
        e.array.accept(this);
        this.say("［");
        e.index.accept(this);
        this.say("]");
        return;
	}

	@Override
	public void visit(Call e) {
		e.exp.accept(this);
		this.say("." + e.id + "(");
		int i = 0;
		for (Exp.T x : e.args) {
			if (i > 0) {
				this.say(", ");
			}
			x.accept(this);
			i++; 
		}
		this.say(")");
		return;
	}

	@Override
	public void visit(False e) {
		this.say("false");
		return;
	}

	@Override
	public void visit(Id e) {
		this.say(e.id);
		return;
	}

	@Override
	public void visit(Length e) {
		e.array.accept(this);
        this.say(".length");
	}

	@Override
	public void visit(Lt e) {
		e.left.accept(this);
		this.say(" < ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(NewIntArray e) {
		this.say("new int[");
		e.exp.accept(this);
		this.say("]");
		return;
	}

	@Override
	public void visit(NewObject e) {
		this.say("new " + e.id + "()");
		return;
	}

	@Override
	public void visit(Not e) {
		this.say("!(");
		e.exp.accept(this);
		this.say(")");
		return;
	}

	@Override
	public void visit(Num e) {
		this.say(String.valueOf(e.num));
		return;
	}

	@Override
	public void visit(Sub e) {
		e.left.accept(this);
		this.say(" - ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(This e) {
		this.say("this");
	}

	@Override
	public void visit(Times e) {
		e.left.accept(this);
		this.say(" * ");
		e.right.accept(this);
		return;
	}

	@Override
	public void visit(True e) {
		this.say("true");
		return;
	}

	// statements
	@Override
	public void visit(Assign s) {
		this.printSpaces();
		this.say(s.id + " = ");
		s.exp.accept(this);
		this.sayln(";");
		return;
	}

	@Override
	public void visit(AssignArray s) {
		this.printSpaces();
		this.say(s.id + "[");
		s.index.accept(this);
		this.say("] = ");
		s.exp.accept(this);
		this.sayln(";");
		return;
	}

	@Override
	public void visit(Block s) {
		for (Stm.T e : s.stms) {
			e.accept(this);
		}
	}

	@Override
	public void visit(If s) {
		this.printSpaces();
		this.say("if (");
		s.condition.accept(this);
		this.say(") ");
		if(s.thenn instanceof Stm.Block)
			this.sayln("{");
		else this.sayln("");
		this.indent();
		this.indent();
		s.thenn.accept(this);
		this.unIndent();
		this.unIndent();
		if (s.thenn instanceof Stm.Block) {
			this.printSpaces();
			this.sayln("}");
		}
		this.printSpaces();
		this.say("else ");
		if(s.elsee instanceof Stm.Block)
			this.sayln("{");
		else this.sayln("");
		this.indent();
		this.indent();
		s.elsee.accept(this);
		this.unIndent();
		this.unIndent();
		if (s.elsee instanceof Stm.Block) {
			this.printSpaces();
			this.sayln("}");
		}
	//	else this.sayln("");
		return;
	}

	@Override
	public void visit(Print s) {
		this.printSpaces();
		this.say("System.out.println(");
		s.exp.accept(this);
		this.sayln(");");
		return;
	}

	@Override
	public void visit(While s) {
		this.printSpaces();
		this.say("While (");
		s.condition.accept(this);
		this.sayln(") {");
		this.indent();
		this.indent();
		s.body.accept(this);
		this.unIndent();
		this.unIndent();
		this.printSpaces();
		this.sayln("}\n");
	}

	// type
	@Override
	public void visit(Boolean t) {
		this.say("boolean");
		return;
	}

	@Override
	public void visit(ClassType t) {
		this.say(t.id);
		return;
	}

	@Override
	public void visit(Int t) {
		this.say("int");
	}

	@Override
	public void visit(IntArray t) {
        this.say("int[]");
	}

	// dec
	@Override
	public void visit(Dec.DecSingle d) {
		d.type.accept(this);
		this.say(" " + d.id);
		return;
	}

	// method
	@Override
	public void visit(MethodSingle m) {
		//每个方法前打印四个空格
		this.printSpaces();
		this.say("public ");
		m.retType.accept(this);
		this.say(" " + m.id + "(");
		//方法形参声明个数 formals
		int i = 0;
		for (Dec.T d : m.formals) {
			if (i > 0) {
				this.say(", ");
			}
			d.accept(this);
			i++;
		} 
		this.say(")");
		this.sayln(" {");
		//打印局部变量声明 locals
		this.indent();
		this.indent();
		for (Dec.T d : m.locals) {
			this.printSpaces();
			d.accept(this);
			this.sayln(";");
		}
		this.sayln("");
		//打印方法的其他语句
		for (Stm.T s : m.stms){
			s.accept(this);
		}
		//打印return语句
		this.printSpaces();
		this.say("return ");
		m.retExp.accept(this);
		this.sayln(";");
		this.unIndent();
		this.unIndent();
		this.printSpaces();
		this.sayln("}\n");
		return;
	}

	// class
	@Override
	public void visit(ClassSingle c) {
		this.say("class " + c.id);
		if (c.extendss != null)
			this.say(" extends " + c.extendss);
		else
			this.say("");

		this.sayln(" {");
		for (Dec.T d : c.decs) {
			this.printSpaces();
			d.accept(this);
			this.sayln(";");
		}
		this.sayln("");
		for (Method.T mthd : c.methods)
			mthd.accept(this);
		this.sayln("}");
		return;
	}

	// main class
	@Override
	public void visit(MainClass.MainClassSingle c) {
		this.say("class " + c.id);
		this.sayln(" {");
		this.printSpaces();
		this.say("public static void main (String[] " + c.arg + ")");
		this.sayln(" {");
		this.indent();
		this.indent();
		c.stm.accept(this);
		this.unIndent();
		this.unIndent();
		this.printSpaces();
		this.sayln("}");
		this.sayln("}");
		return;
	}

	// program
	@Override
	public void visit(Program.ProgramSingle p) {
		p.mainClass.accept(this);
		this.sayln("");
		for (ast.Ast.Class.T classs : p.classes) {
			classs.accept(this);
		}
		System.out.println("\n\n");
	}
}
