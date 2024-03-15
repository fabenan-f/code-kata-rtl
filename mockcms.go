package main

import (
	"log"
	"net/http"
)

func main() {
	http.HandleFunc("/cms/create", postArticles)
	err := http.ListenAndServe("0.0.0.0:3333", nil)
	if err != nil {
		log.Fatalf("Could not start server because %v", err)
	}
	log.Println("Mock CMS is listening")
}

func postArticles(w http.ResponseWriter, r *http.Request) {
	statusCode := http.StatusInternalServerError
	log.Printf("Received message, will write status %d\n", statusCode)
	w.WriteHeader(statusCode)
}
