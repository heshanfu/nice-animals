package com.grohden.niceanimals.services

import com.grohden.niceanimals.realm.entities.NiceAnimal
import com.grohden.niceanimals.shibe.service.AnimalType
import com.grohden.niceanimals.shibe.service.ShibeService
import io.realm.Realm
import java.net.URL
import java.util.concurrent.CompletableFuture

class NiceAnimalsService(private var shibeService: ShibeService) {

    private fun persistListIntoRealm(animals: List<NiceAnimal>) {
        try {
            val realm: Realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            realm.copyToRealm(animals)
            realm.commitTransaction()
        } catch (e: Exception) {
            print(e);
        }

    }

    private fun buildAnimalsFromList(urlList: List<URL>, type: AnimalType): List<NiceAnimal> {
        return urlList
                .map { it.toString() }
                .map { NiceAnimal(it, type) }
                .toList()
    }

    /**
     * Fetches all types of animals from shibe api
     *
     * @return a completable future with those new animals to chain into another operation
     */
    private fun fetchAllTypes(): CompletableFuture<List<NiceAnimal>> {
        return fetchMoreAnimals(AnimalType.shibes)
                .thenCombineAsync(fetchMoreAnimals(AnimalType.birds)) { listOne, listTwo -> listOne + listTwo }
                .thenCombineAsync(fetchMoreAnimals(AnimalType.cats)) { listOne, listTwo -> listOne + listTwo }
    }

    /**
     * Fetches more animals and remove all the old ones before putting the new ones into
     * realm
     *
     * @return a completable future with those new animals to chain into another operation
     */
    fun refreshAnimals(): CompletableFuture<List<NiceAnimal>> {
        return fetchAllTypes()
                .thenApply { animals ->
                    Realm.getDefaultInstance().executeTransaction { realm ->
                        realm.delete(NiceAnimal::class.java)
                        realm.copyToRealm(animals.shuffled())
                    }
                    animals
                }
    }

    /**
     * Fetches more animals
     *
     * @param type  type of the animal to be fetched
     * @param count quantity of animals
     * @return a completable future with those new animals to chain into another operation
     */
    private fun fetchMoreAnimals(type: AnimalType, count: Int = DEFAULT_IMAGE_FETCH_COUNT): CompletableFuture<List<NiceAnimal>> {
        val future = shibeService.fetchNiceImageUrls(
                type,
                count
        )

        return future.thenApplyAsync { this.buildAnimalsFromList(it, type) }
    }

    /**
     * Fetches and persists all three types of animals into realm, shuffling them before
     * persisting
     *
     * @return a completable future with those new animals to chain into another operation
     */
    fun fetchAndPersistAllTypes(): CompletableFuture<List<NiceAnimal>> {

        return fetchAllTypes()
                .thenApply { animals ->
                    persistListIntoRealm(animals.shuffled())
                    animals
                }
    }

    /**
     * Fetches and persists new animals into realm
     *
     * @param type  type of the animal to be fetched and persisted
     * @param count quantity of animals
     * @return a completable future with those new animals to chain into another operation
     */
    fun fetchAndPersistMore(type: AnimalType, count: Int = DEFAULT_IMAGE_FETCH_COUNT): CompletableFuture<List<NiceAnimal>> {

        return fetchMoreAnimals(type, count).thenApplyAsync { animals ->
            persistListIntoRealm(animals)
            animals
        }
    }

    companion object {
        private const val DEFAULT_IMAGE_FETCH_COUNT = 10
    }
}