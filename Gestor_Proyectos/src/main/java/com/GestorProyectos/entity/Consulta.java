package com.GestorProyectos.entity;

import java.io.Serializable;

public class Consulta implements Serializable{

	
	private String idConsulta;

	private String nombre;
	private String autor;
	private long numeroVisitante;
	
	public Consulta(String id, String name, String owner, long l) {
		this.idConsulta=id;
		this.nombre=name;
		this.autor=owner;
		this.numeroVisitante=l;
	}
	public Consulta() {
		// TODO Auto-generated constructor stub
	}
	public String getIdConsulta() {
		return idConsulta;
	}
	public void setIdConsulta(String id) {
		this.idConsulta = id;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getAutor() {
		return autor;
	}
	public void setAutor(String autor) {
		this.autor = autor;
	}
	public long getNumeroVisitante() {
		return numeroVisitante;
	}
	public void setNumeroVisitante(long numeroVisitante) {
		this.numeroVisitante = numeroVisitante;
	}
	public void setContent(String url) {
		String []aux=url.split("/");
    	if(aux.length>2) {
    		this.idConsulta=String.valueOf((int)(Math.random()*10000));
    		this.nombre=aux[2];
        	this.autor=aux[1];
    		this.numeroVisitante = 0;
    	}
		
	}
	
}

	