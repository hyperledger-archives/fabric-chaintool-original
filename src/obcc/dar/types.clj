(ns obcc.dar.types
  (:import [obcc.dar
            Dar$CompatibilityHeader
            Dar$Archive
            Dar$Archive$Signature
            Dar$Archive$Payload
            Dar$Archive$Payload$Compression
            Dar$Archive$Payload$Entries])
  (:require [flatland.protobuf.core :as fl]))

(def Header      (fl/protodef Dar$CompatibilityHeader))
(def Archive     (fl/protodef Dar$Archive))
(def Signature   (fl/protodef Dar$Archive$Signature))
(def Payload     (fl/protodef Dar$Archive$Payload))
(def Compression (fl/protodef Dar$Archive$Payload$Compression))
(def Entries     (fl/protodef Dar$Archive$Payload$Entries))
