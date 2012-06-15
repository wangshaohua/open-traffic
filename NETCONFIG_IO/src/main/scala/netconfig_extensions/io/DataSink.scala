package netconfig_extensions.io
/**
 * Objects that store data sequentially.
 *
 * @author tjhunter
 *
 * @type DatumType: the type of datum that you can store in this collection.
 */
trait DataSink[-DatumType] {

  /**
   * Inserts the datum in the sink.
   */
  def put(datum: DatumType)

  /**
   * Indicates that no other datum will be pushed in the sink, so that
   * optional closing operations may be performed.
   */
  def close(): Unit
}
