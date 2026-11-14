package com.carmotors.modelo;

public class cliente {
    private int id ;
    private String nombre ;
    private String documento;
    private String telefono;
    private String email;

    public cliente (int id , String nombre , String documento, String telefono , String email){
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.telefono = telefono;
        this.email = email;
    }

}
