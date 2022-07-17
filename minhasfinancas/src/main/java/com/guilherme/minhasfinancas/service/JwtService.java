package com.guilherme.minhasfinancas.service;

import com.guilherme.minhasfinancas.model.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

public interface JwtService {

	String gerarToken(Usuario usuario);
	
	Claims obterClaims(String token) throws ExpiredJwtException;
	
	boolean isTokenValido(String token);
	
	String obterLoginUsuario( String token );
}
