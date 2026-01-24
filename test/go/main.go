package main

import (
	"log"
	"os"
)

func main() {
	log.Printf("MISE_GO: %s", os.Getenv("MISE_GO"))
}
