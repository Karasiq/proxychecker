package com.karasiq.proxychecker.store.mapdb

import java.io.{DataInput, DataOutput}

import com.karasiq.mapdb.MapDbWrapper
import com.karasiq.mapdb.MapDbWrapper._
import com.karasiq.mapdb.serialization.MapDbSerializer
import com.karasiq.mapdb.serialization.MapDbSerializer.Default._
import com.karasiq.proxychecker.store._
import org.mapdb.Serializer

private[mapdb] object MapDbProxyCollection {
  @inline
  private def nameFor(listName: String): String = {
    "list$" + listName
  }

  def apply(listName: String): ProxyCollection = {
    new MapDbProxyCollection(nameFor(listName))
  }

  def entryMap(name: String): MapDbTreeMap[String, ProxyStoreEntry] = {
    val mapDb = ProxyCheckerMapDb()

    MapDbWrapper(mapDb).createTreeMap(name)(_
      .keySerializer(MapDbSerializer[String])
      .valueSerializer(MapDbSerializer[ProxyStoreEntry])
      .nodeSize(32)
      .valuesOutsideNodesEnable()
    )
  }

  def listMap(): MapDbHashMap[String, ProxyList] = {
    def proxyListSerializer: Serializer[ProxyList] = new Serializer[ProxyList] {
      override def serialize(out: DataOutput, value: ProxyList): Unit = {
        MapDbSerializer[String].serialize(out, value.name)
        MapDbSerializer[Set[String]].serialize(out, value.sources)
      }

      override def deserialize(in: DataInput, available: Int): ProxyList = {
        val name = MapDbSerializer[String].deserialize(in, available)
        ProxyList(name, MapDbSerializer[Set[String]].deserialize(in, available), MapDbProxyCollection(name))
      }
    }

    MapDbWrapper(ProxyCheckerMapDb()).createHashMap("proxyStore")(_
      .keySerializer(MapDbSerializer[String])
      .valueSerializer(proxyListSerializer)
    )
  }
}

final private class MapDbProxyCollection(name: String) extends ProxyCollectionImpl(MapDbProxyCollection.entryMap(name)) with Serializable

final class MapDbProxyStore extends ProxyStoreImpl(MapDbProxyCollection.listMap()) {
  override def createList(name: String): ProxyList = {
    assert(!contains(name), "List already exists: " + name)
    val collection = MapDbProxyCollection(name)
    val list = ProxyList(name, Set(), collection)
    this += (name → list)
    list
  }
}
